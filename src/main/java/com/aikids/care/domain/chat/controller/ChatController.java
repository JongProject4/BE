package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*") // 모든 곳에서 오는 요청 허용(임시 프론트 테스트용)
@RestController // API 요청 받는 컨트롤러
@RequestMapping("/api/chats") // "/api/chats"만 받음
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    //API 부모가 메시지 전송 -> DB 저장 -> AI 답변 반환
    @PostMapping("/{chat_id}/messages")
    public String sendMessage(
            @PathVariable("chat_id") Long chatId,
            @RequestBody ChatMessageRequest request) { // @RequestBody: JSON을 DTO로 변환해서 받음

        // 아이디랑 요청을 넘김
        String aiResponse = chatService.sendMessage(chatId, request);

        //AI 답변 텍스트
        return aiResponse;
    }
}