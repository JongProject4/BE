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
import com.aikids.care.infra.stt.GoogleSttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final GeminiService geminiService;
    private final GoogleSttClient googleSttClient;
    private final TransactionTemplate transactionTemplate;

    // 1. 새로운 상담 세션(빈 방) 생성
    @Transactional
    public Long createChat(ChatCreateRequest request) {
        Chat chat = Chat.builder()
                .childId(request.getChildId())
                .build();
        return chatRepository.save(chat).getId();
    }

    // 2. 메시지 전송 및 AI 답변 받기
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
        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.USER)
                    .content(userContent)
                    .imageUrl(imageUrl)
                    .build());
            return null;
        });

        String aiContent = geminiService.askQuestion(userContent, imageUrl);

        transactionTemplate.execute(status -> {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));
            chatDetailRepository.save(ChatDetail.builder()
                    .chat(chat)
                    .role(Role.AI)
                    .content(aiContent)
                    .build());
            return null;
        });

        return aiContent;
    }

    // 3. AI 분석 결과 업데이트 (PATCH API)
    @Transactional
    public void updateChatResult(Long chatId, ChatUpdateRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));

        chat.updateResult(request.getCategory(), request.getRiskLevel());
    }

    // 4. 채팅방 삭제
    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));
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
                .collect(Collectors.toList());
    }

    public List<Long> getChatRoomList(Long childId) {
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(Chat::getId)
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
}
