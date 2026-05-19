package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatCreateResponse;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.dto.ChatMessageResponse;
import com.aikids.care.domain.chat.dto.ChatUpdateRequest;
import com.aikids.care.domain.chat.dto.VoiceChatResponse;
import com.aikids.care.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ChatMessageResponse> sendMessage(@PathVariable Long chatId,
                                                           @RequestBody ChatMessageRequest request) {
        String aiAnswer = chatService.sendMessage(chatId, request);
        return ResponseEntity.ok(new ChatMessageResponse(aiAnswer));
    }

    // 음성 파일을 받아 STT -> LLM 답변까지 처리하는 API (POST /api/chats/{chatId}/voices)
    @PostMapping("/{chatId}/voices")
    public ResponseEntity<VoiceChatResponse> sendVoiceMessage(@PathVariable Long chatId,
                                                              @RequestParam("file") MultipartFile file) throws Exception {
        VoiceChatResponse response = chatService.sendVoiceMessage(chatId, file);
        return ResponseEntity.ok(response);
    }

    // 특정 아이(childId)의 상담 방 목록 가져오기 API
    @GetMapping("/rooms/list/{childId}")
    public ResponseEntity<List<Long>> getChatRoomList(@PathVariable Long childId) {
        List<Long> roomIds = chatService.getChatRoomList(childId);
        return ResponseEntity.ok(roomIds);
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
}
