package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.dto.ChatUpdateRequest;
import com.aikids.care.domain.chat.dto.VoiceChatResponse;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 40; // 최근 20턴

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final GeminiApiClient geminiApiClient;
    private final GoogleSttClient googleSttClient;
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
        // Gemini 호출 전 기존 대화 히스토리 조회 (현재 메시지 제외)
        List<Map<String, String>> history = loadHistory(chatId);

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

        String aiContent = geminiApiClient.ask(userContent, history);

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

    public void closeChat(Long chatId) {
        List<Map<String, String>> history = loadHistory(chatId);
        if (history.isEmpty()) return;

        String summary = geminiApiClient.summarize(history);

        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
            chat.updateSummary(summary);
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
}
