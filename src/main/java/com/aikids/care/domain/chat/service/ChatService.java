package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.dto.VoiceChatResponse;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.infra.gemini.GeminiApiClient;
import com.aikids.care.infra.stt.GoogleSttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ObjectProvider<GeminiApiClient> geminiApiClientProvider;
    private final GoogleSttClient googleSttClient;

    // 새로운 상담 세션 생성 API
    @Transactional
    public Long createChat(ChatCreateRequest request) {
        Chat newChat = Chat.builder()
                .childId(request.getChildId())
                .build();
        Chat savedChat = chatRepository.save(newChat);
        return savedChat.getId();
    }

    // 메시지 전송 및 AI 답변 받기 API
    @Transactional
    public String sendMessage(Long chatId, ChatMessageRequest request) {
        return sendTextMessage(chatId, request.getContent());
    }

    @Transactional
    public VoiceChatResponse sendVoiceMessage(Long chatId, MultipartFile audioFile) throws IOException {
        String userQuestion = googleSttClient.transcribe(audioFile.getBytes());
        log.info("[STT] chatId={}, transcript='{}'", chatId, abbreviate(userQuestion, 200));
        log.info("[STT-UNICODE] chatId={}, transcript='{}'", chatId, toUnicodeEscapes(abbreviate(userQuestion, 200)));
        if (userQuestion.isBlank()) {
            return new VoiceChatResponse("", "음성을 인식하지 못했습니다. 다시 말해 주세요.");
        }
        String aiAnswer = sendTextMessage(chatId, userQuestion);
        return new VoiceChatResponse(userQuestion, aiAnswer);
    }

    private String sendTextMessage(Long chatId, String userContent) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담 세션을 찾을 수 없습니다."));

        ChatDetail userMessage = ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(userContent)
                .build();
        chatDetailRepository.save(userMessage);

        GeminiApiClient gemini = geminiApiClientProvider.getIfAvailable();
        String aiResponseText = gemini != null
                ? gemini.askToGemini(userContent)
                : "[AI 비활성화] spring.ai.model.chat 가 none 입니다. "
                        + "Gemini를 쓰려면 gemini 프로필과 GEMINI_API_KEY를 설정하세요.";

        ChatDetail aiMessage = ChatDetail.builder()
                .chat(chat)
                .role(Role.AI)
                .content(aiResponseText)
                .build();
        chatDetailRepository.save(aiMessage);

        return aiResponseText;
    }

    @Transactional(readOnly = true)
    public List<Long> getChatRoomList(Long childId) {
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(Chat::getId)
                .collect(Collectors.toList());
    }

    //특정 상담 방(ChatId) 안의 '모든 대화 내역(ChatDetail)' 가져오기
    @Transactional(readOnly = true)
    public List<ChatDetailResponse> getChatHistory(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담 세션을 찾을 수 없습니다."));

        return chatDetailRepository.findByChatOrderByCreatedAtAsc(chat)
                .stream()
                .map(detail -> new ChatDetailResponse(
                        detail.getRole(),
                        detail.getContent(),
                        detail.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String toUnicodeEscapes(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }
}