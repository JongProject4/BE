package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.alarm.AlarmFlowHandler;
import com.aikids.care.domain.chat.dto.*;
import com.aikids.care.domain.chat.model.*;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import com.aikids.care.infra.gemini.GeminiApiClient;
import com.aikids.care.infra.stt.GoogleSttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20; // 최근 10턴
    private static final int INTERIM_SUMMARY_TRIGGER = 30; // 이 수 이상이면 중간 요약 생성

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ChatMessagePersistence chatMessagePersistence;
    private final GoogleSttClient googleSttClient;
    private final VoiceChatStreamPipeline voiceChatStreamPipeline;
    private final GeminiApiClient geminiApiClient;
    private final ChatAiService chatAiService;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final AlarmFlowHandler alarmFlowHandler;

    @Transactional
    public Long createChat(String socialId, SocialType socialType, ChatCreateRequest request) {
        User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        childRepository.findByIdAndUser_Id(request.getChildId(), user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        Chat chat = Chat.builder()
                .childId(request.getChildId())
                .build();
        return chatRepository.save(chat).getId();
    }

    public String sendMessage(Long chatId, String socialId, SocialType socialType, ChatMessageRequest request) {
        ChatContext ctx = loadValidatedContext(chatId, socialId, socialType);
        Optional<String> alarmReply = tryAlarmFlow(chatId, ctx.userId(), ctx.childId(), request.getContent());
        if (alarmReply.isPresent()) {
            persistAlarmTurn(chatId, request.getContent(), request.getImageUrl(), alarmReply.get());
            return alarmReply.get();
        }
        return sendInternalMessage(chatId, request.getContent(), request.getImageUrl(), false);
    }

    public VoiceChatResponse sendVoiceMessage(Long chatId, String socialId, SocialType socialType, MultipartFile audioFile) throws IOException {
        ChatContext ctx = loadValidatedContext(chatId, socialId, socialType);
        String userQuestion = googleSttClient.transcribe(
                audioFile.getBytes(),
                audioFile.getContentType(),
                audioFile.getOriginalFilename()
        );
        log.info("[STT] chatId={}, transcript='{}'", chatId, abbreviate(userQuestion, 200));
        if (userQuestion.isBlank()) {
            return new VoiceChatResponse("", "음성을 인식하지 못했습니다. 다시 말해 주세요.");
        }
        Optional<String> alarmReply = tryAlarmFlow(chatId, ctx.userId(), ctx.childId(), userQuestion);
        if (alarmReply.isPresent()) {
            persistAlarmTurn(chatId, userQuestion, null, alarmReply.get());
            return new VoiceChatResponse(userQuestion, alarmReply.get());
        }
        String aiAnswer = sendInternalMessage(chatId, userQuestion, null, true);
        return new VoiceChatResponse(userQuestion, aiAnswer);
    }

    /**
     * 음성 상담 SSE 스트리밍: STT → 짧은 답변 텍스트/오디오 청크 → 종료 시 DB 저장.
     * 알람 등록 흐름이면 스트리밍·TTS를 건너뛰고 단발 텍스트 응답으로 마감한다.
     */
    public Flux<ChatStreamResponse> handleVoiceChatStream(Long chatId, String socialId, SocialType socialType, MultipartFile voiceFile) {
        ChatContext ctx = loadValidatedContext(chatId, socialId, socialType);
        return Mono.fromCallable(() -> googleSttClient.transcribe(
                        voiceFile.getBytes(),
                        voiceFile.getContentType(),
                        voiceFile.getOriginalFilename()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(transcript -> handleTranscript(chatId, ctx, transcript))
                .onErrorResume(IOException.class, e -> {
                    log.error("[VoiceStream] STT failed chatId={}", chatId, e);
                    return Flux.just(
                            ChatStreamResponse.ofChunk("음성 인식에 실패했습니다. 다시 시도해 주세요.", ""),
                            ChatStreamResponse.ofFinal()
                    );
                });
    }

    private Flux<ChatStreamResponse> handleTranscript(Long chatId, ChatContext ctx, String transcript) {
        if (transcript.isBlank()) {
            return Flux.just(
                    ChatStreamResponse.ofChunk("음성을 인식하지 못했습니다. 다시 말해 주세요.", ""),
                    ChatStreamResponse.ofFinal()
            );
        }
        Optional<String> alarmReply = tryAlarmFlow(chatId, ctx.userId(), ctx.childId(), transcript);
        if (alarmReply.isPresent()) {
            return emitAlarmReplyStream(chatId, transcript, alarmReply.get());
        }
        return buildVoiceStream(chatId, transcript);
    }

    private Flux<ChatStreamResponse> emitAlarmReplyStream(Long chatId, String transcript, String reply) {
        return Mono.fromRunnable(() -> {
                    chatMessagePersistence.saveUserTranscript(chatId, transcript);
                    chatMessagePersistence.saveAiReply(chatId, reply);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenMany(Flux.just(
                        ChatStreamResponse.ofFirstChunk(transcript, reply, ""),
                        ChatStreamResponse.ofFinal()
                ));
    }

    private Flux<ChatStreamResponse> buildVoiceStream(Long chatId, String transcript) {
        log.info("[VoiceStream] chatId={}, transcript='{}'", chatId, abbreviate(transcript, 200));

        if (transcript.isBlank()) {
            return Flux.just(
                    ChatStreamResponse.ofChunk("음성을 인식하지 못했습니다. 다시 말해 주세요.", ""),
                    ChatStreamResponse.ofFinal()
            );
        }

        // 현재 질문 저장 전에 히스토리 로드 (저장 후 로드하면 현재 턴이 히스토리에 중복 포함됨)
        List<ChatDetail> allDetails = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        String interimSummary = generateInterimSummaryIfNeeded(chatId, allDetails);
        List<Map<String, String>> history = toHistory(allDetails);

        Long childId = chatRepository.findById(chatId)
                .map(Chat::getChildId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        String childInfo = formatChildInfo(childId);

        return Mono.fromRunnable(() -> chatMessagePersistence.saveUserTranscript(chatId, transcript))
                .subscribeOn(Schedulers.boundedElastic())
                .thenMany(voiceChatStreamPipeline.stream(chatId, transcript, history, interimSummary, childInfo));
    }

    // LLM 호출 구간에 DB 커넥션을 점유하지 않도록 TransactionTemplate으로 트랜잭션 분리
    private String sendInternalMessage(Long chatId, String userContent, String imageUrl, boolean isVoice) {
        List<ChatDetail> allDetails = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        String interimSummary = generateInterimSummaryIfNeeded(chatId, allDetails);
        List<Map<String, String>> history = toHistory(allDetails);

        Long childId = chatRepository.findById(chatId)
                .map(Chat::getChildId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        String childInfo = formatChildInfo(childId);

        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.USER)
                    .content(userContent)
                    .imageUrl(imageUrl)
                    .build());
            return null;
        });

        String aiContent = isVoice
                ? geminiApiClient.askVoice(userContent, history, interimSummary, childInfo)
                : geminiApiClient.askText(userContent, history, interimSummary, childInfo);

        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.AI)
                    .content(aiContent)
                    .build());
            return null;
        });

        return aiContent;
    }

    // 히스토리가 INTERIM_SUMMARY_TRIGGER 이상이면 오래된 구간을 요약하고 DB에 저장
    private String generateInterimSummaryIfNeeded(Long chatId, List<ChatDetail> allDetails) {
        if (allDetails.size() < INTERIM_SUMMARY_TRIGGER) {
            return chatRepository.findById(chatId)
                    .map(Chat::getInterimSummary)
                    .orElse(null);
        }

        // 오래된 절반을 요약 대상으로, 최근 절반은 슬라이딩 윈도우로 유지
        int splitAt = allDetails.size() - MAX_HISTORY_MESSAGES;
        List<Map<String, String>> oldMessages = allDetails.subList(0, splitAt).stream()
                .map(d -> Map.of("role", toGeminiRole(d.getRole()), "content", d.getContent()))
                .toList();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        String previousSummary = chat.getInterimSummary();
        String newSummary = geminiApiClient.summarize(oldMessages, previousSummary);

        transactionTemplate.execute(status -> {
            Chat c = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            c.updateInterimSummary(newSummary);
            return null;
        });

        return newSummary;
    }

    public void closeChat(Long chatId, String socialId, SocialType socialType) {
        validateChatOwnership(chatId, socialId, socialType);
        List<Map<String, String>> history = loadHistory(chatId);
        if (history.isEmpty()) return;

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        String summary = geminiApiClient.summarize(history, chat.getInterimSummary());

        transactionTemplate.execute(status -> {
            Chat c = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            c.updateSummary(summary);
            return null;
        });
    }

    @Transactional
    public void updateChatResult(Long chatId, String socialId, SocialType socialType, ChatUpdateRequest request) {
        validateChatOwnership(chatId, socialId, socialType);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        chat.updateResult(request.getCategory(), request.getRiskLevel());
    }

    @Transactional
    public void deleteChat(Long chatId, String socialId, SocialType socialType) {
        validateChatOwnership(chatId, socialId, socialType);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        chatRepository.delete(chat);
    }

    public List<ChatDetailResponse> getChatHistory(Long chatId, String socialId, SocialType socialType) {
        validateChatOwnership(chatId, socialId, socialType);
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return details.stream()
                .map(detail -> new ChatDetailResponse(
                        detail.getId(),
                        detail.getRole(),
                        detail.getContent(),
                        detail.getImageUrl(),
                        detail.getCreatedAt()
                ))
                .toList();
    }

    public List<Long> getChatRoomList(String socialId, SocialType socialType, Long childId) {
        User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        childRepository.findByIdAndUser_Id(childId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(Chat::getId)
                .toList();
    }

    private List<Map<String, String>> loadHistory(Long chatId) {
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return toHistory(details);
    }

    private List<Map<String, String>> toHistory(List<ChatDetail> details) {
        List<ChatDetail> recent = details.size() > MAX_HISTORY_MESSAGES
                ? details.subList(details.size() - MAX_HISTORY_MESSAGES, details.size())
                : details;
        return recent.stream()
                .map(d -> Map.of("role", toGeminiRole(d.getRole()), "content", d.getContent()))
                .toList();
    }

    private String toGeminiRole(Role role) {
        return role == Role.USER ? "user" : "model";
    }

    private String formatChildInfo(Long childId) {
        if (childId == null) return null;
        return childRepository.findById(childId)
                .map(child -> {
                    long months = ChronoUnit.MONTHS.between(
                            child.getBirthdate().toLocalDate(), LocalDate.now());
                    String age = months < 12 ? months + "개월" : (months / 12) + "세";
                    String history = (child.getMedicalHistory() == null || child.getMedicalHistory().isBlank())
                            ? "없음" : child.getMedicalHistory();
                    String allergies = (child.getAllergies() == null || child.getAllergies().isBlank())
                            ? "없음" : child.getAllergies();
                    return """
                            [상담 대상 아이 정보 - 반드시 참고하여 답변하세요]
                            - 이름: %s
                            - 나이: %s
                            - 병력: %s
                            - 알러지: %s
                            """.formatted(child.getName(), age, history, allergies);
                })
                .orElse(null);
    }

    private void validateChatOwnership(Long chatId, String socialId, SocialType socialType) {
        loadValidatedContext(chatId, socialId, socialType);
    }

    private ChatContext loadValidatedContext(Long chatId, String socialId, SocialType socialType) {
        User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        childRepository.findByIdAndUser_Id(chat.getChildId(), user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        return new ChatContext(user.getId(), chat.getChildId());
    }

    private Optional<String> tryAlarmFlow(Long chatId, Long userId, Long childId, String message) {
        try {
            return alarmFlowHandler.handle(chatId, userId, childId, message);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alarm] flow failed chatId={}", chatId, e);
            return Optional.of("알람 등록 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void persistAlarmTurn(Long chatId, String userContent, String imageUrl, String aiContent) {
        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.USER)
                    .content(userContent)
                    .imageUrl(imageUrl)
                    .build());
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.AI)
                    .content(aiContent)
                    .build());
            return null;
        });
    }

    private record ChatContext(Long userId, Long childId) {
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, maxLength) + "...";
    }

    @Transactional
    public AiAnalysisResponse analyzeChatAndSave(Long chatId, String socialId, SocialType socialType) {
        validateChatOwnership(chatId, socialId, socialType);
        // 해당 방의 상세 대화 내역 조회 (IsUser가 true면 부모, false면 AI)
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        String chatHistory = details.stream()
                .map(d -> (d.getRole()== Role.USER? "부모: " : "AI: ") + d.getContent())
                .collect(Collectors.joining("\n"));

        // AI 서비스 호출 (반환 타입 수정됨)
        AiAnalysisResponse result = chatAiService.analyzeContent(chatHistory);

        // DB 저장 (엔티티의 필드명에 맞춰 수정하세요)
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        // chat 엔티티에 분석 결과를 업데이트하는 메서드가 필요합니다.
        chat.updateAnalysis(
                Category.valueOf(result.getCategory().toUpperCase()),
                RiskLevel.valueOf(result.getRiskLevel().toUpperCase())
        );

        return result;
    }
}
