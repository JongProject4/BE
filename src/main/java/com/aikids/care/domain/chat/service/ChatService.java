package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.dto.ChatUpdateRequest;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final GeminiService geminiService;

    // 1. 새로운 상담 세션(빈 방) 생성
    @Transactional
    public Long createChat(ChatCreateRequest request) {
        Chat chat = Chat.builder()
                .childId(request.getChildId())
                .build();
        return chatRepository.save(chat).getId();
    }

    // 2. 메시지 전송 및 AI 답변 받기
    @Transactional
    public String sendMessage(Long chatId, ChatMessageRequest request) {
        // 1) 채팅방 찾기
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));

        // 2) 부모가 보낸 메시지(사진 포함)를 DB에 저장
        ChatDetail userMsg = ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build();
        chatDetailRepository.save(userMsg);

        // 3) AI에게 물어보고 답변 받기
        String aiContent = geminiService.askQuestion(request.getContent(), request.getImageUrl());

        // 4) AI의 답변을 DB에 저장
        ChatDetail aiMsg = ChatDetail.builder()
                .chat(chat)
                .role(Role.AI)
                .content(aiContent)
                .build();
        chatDetailRepository.save(aiMsg);

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

        // 1. 해당 채팅방(chatId)의 대화 내역을 시간순(오래된 것 -> 최신 것)으로 가져옵니다.
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // 2. DB 엔티티(ChatDetail)를 프론트엔드용 DTO(ChatDetailResponse)로 변환해서 리스트로 묶어 반환합니다.
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
    // 특정 아이의 상담 방 번호 목록을 최신순으로 조회
    public List<Long> getChatRoomList(Long childId) {
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(chat -> chat.getId())
                .collect(Collectors.toList());
    }
}