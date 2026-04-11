package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatCreateResponse;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    //새로운 상담 방 만들기 API
    @PostMapping("/rooms")
    public ResponseEntity<ChatCreateResponse> createChat(@RequestBody ChatCreateRequest request) {
        Long chatId = chatService.createChat(request);

        return ResponseEntity.ok(new ChatCreateResponse(chatId));
    }

    // 특정 방에 메시지 보내고 AI 답변 받기 API
    @PostMapping("/rooms/{chatId}/messages")
    public ResponseEntity<String> sendMessage(
            @PathVariable Long chatId,
            @RequestBody ChatMessageRequest request) {
        String aiAnswer = chatService.sendMessage(chatId, request);
        return ResponseEntity.ok(aiAnswer);
    }

    // 특정 아이(childId)의 상담 방 목록 가져오기 API
    @GetMapping("/rooms/list/{childId}")
    public ResponseEntity<List<Long>> getChatRoomList(@PathVariable Long childId) {
        List<Long> roomIds = chatService.getChatRoomList(childId);
        return ResponseEntity.ok(roomIds);
    }

    // 특정 방(chatId)의 과거 대화 내역(ChatDetail) 가져오기 API
    @GetMapping("/rooms/{chatId}/messages")
    public ResponseEntity<List<ChatDetailResponse>> getChatHistory(@PathVariable Long chatId) {
        List<ChatDetailResponse> history = chatService.getChatHistory(chatId);
        return ResponseEntity.ok(history);
    }
}