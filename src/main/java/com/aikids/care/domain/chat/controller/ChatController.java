package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.*;
import com.aikids.care.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 1. 새로운 AI 상담 세션 생성 (POST /api/chat)
    @PostMapping
    public ResponseEntity<ChatCreateResponse> createChat(@RequestBody ChatCreateRequest request) {
        Long chatId = chatService.createChat(request);
        return ResponseEntity.ok(new ChatCreateResponse(chatId));
    }

    // 2. 부모 메시지 전송 및 AI 답변 반환 (POST /api/chat/{chat_id}/messages)
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage( // 반환 타입 변경
                                                            @PathVariable Long chatId,
                                                            @RequestBody ChatMessageRequest request) {
        String aiAnswer = chatService.sendMessage(chatId, request);
        return ResponseEntity.ok(new ChatMessageResponse(aiAnswer)); // 객체로 감싸서 반환
    }

    // 3. 상담 세션 분석 결과 업데이트 (PATCH /api/chat/{chat_id})
    @PatchMapping("/{chatId}")
    public ResponseEntity<Void> updateChatResult(@PathVariable Long chatId, @RequestBody ChatUpdateRequest request) {
        chatService.updateChatResult(chatId, request);
        return ResponseEntity.ok().build();
    }

    // 4. 특정 상담 세션의 모든 대화 내용 조회 (GET /api/chat/{chat_id}/messages)
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatDetailResponse>> getChatHistory(@PathVariable Long chatId) {
        List<ChatDetailResponse> history = chatService.getChatHistory(chatId);
        return ResponseEntity.ok(history);
    }

    // 5. 상담 세션 삭제 (DELETE /api/chat/{chat_id})
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/analyze")
    public ResponseEntity<Void> analyzeChat(@PathVariable Long chatId) {
        chatService.analyzeAndCloseConsultation(chatId);
        return ResponseEntity.ok().build(); // 200 OK 만 반환
    }
}