package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.infra.gemini.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ObjectProvider<GeminiApiClient> geminiApiClientProvider;

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
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담 세션을 찾을 수 없습니다."));

        ChatDetail userMessage = ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(request.getContent())
                .build();
        chatDetailRepository.save(userMessage);

        GeminiApiClient gemini = geminiApiClientProvider.getIfAvailable();
        String aiResponseText = gemini != null
                ? gemini.askToGemini(request.getContent())
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
}