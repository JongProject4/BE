package com.aikids.care.domain.chat.service;

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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20; // 최근 10턴
    private static final int INTERIM_SUMMARY_TRIGGER = 30; // 이 수 이상이면 중간 요약 생성

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final GeminiApiClient geminiApiClient;
    private final GoogleSttClient googleSttClient;
    private final ChatAiService chatAiService;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;

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

    public String sendMessage(Long chatId, ChatMessageRequest request) {
        return sendTextMessage(chatId, request.getContent(), request.getImageUrl());
    }

    public VoiceChatResponse sendVoiceMessage(Long chatId, MultipartFile audioFile) throws IOException {
        String userQuestion = googleSttClient.transcribe(audioFile.getBytes());
        log.info("[STT] chatId={}, transcript='{}'", chatId, abbreviate(userQuestion, 200));
        if (userQuestion.isBlank()) {
            return new VoiceChatResponse("", "음성을 인식하지 못했습니다. 다시 말해 주세요.");
        }
        String aiAnswer = sendTextMessage(chatId, userQuestion, null);
        return new VoiceChatResponse(userQuestion, aiAnswer);
    }

    // LLM 호출 구간에 DB 커넥션을 점유하지 않도록 TransactionTemplate으로 트랜잭션 분리
    private String sendTextMessage(Long chatId, String userContent, String imageUrl) {
        List<ChatDetail> allDetails = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        String interimSummary = generateInterimSummaryIfNeeded(chatId, allDetails);
        List<Map<String, String>> history = toHistory(allDetails);

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

        String aiContent = geminiApiClient.ask(userContent, history, interimSummary);

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

    public void closeChat(Long chatId) {
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
    public void updateChatResult(Long chatId, ChatUpdateRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        chat.updateResult(request.getCategory(), request.getRiskLevel());
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        chatRepository.delete(chat);
    }

    public List<ChatDetailResponse> getChatHistory(Long chatId) {
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

    private String abbreviate(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, maxLength) + "...";
    }

    @Transactional
    public AiAnalysisResponse analyzeChatAndSave(Long chatId) {
        // 해당 방의 상세 대화 내역 조회 (IsUser가 true면 부모, false면 AI)
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        String chatHistory = details.stream()
                .map(d -> (d.getRole()== Role.USER? "부모: " : "AI: ") + d.getContent())
                .collect(Collectors.joining("\n"));

        // AI 서비스 호출 (반환 타입 수정됨)
        AiAnalysisResponse result = chatAiService.analyzeContent(chatHistory);

        // DB 저장 (엔티티의 필드명에 맞춰 수정하세요)
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));

        // chat 엔티티에 분석 결과를 업데이트하는 메서드가 필요합니다.
        chat.updateAnalysis(
                Category.valueOf(result.getCategory().toUpperCase()),
                RiskLevel.valueOf(result.getRiskLevel().toUpperCase())
        );

        return result;
    }
}
