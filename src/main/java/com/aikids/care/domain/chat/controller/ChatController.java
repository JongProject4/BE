package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.ChatCreateRequest;
import com.aikids.care.domain.chat.dto.ChatCreateResponse;
import com.aikids.care.domain.chat.dto.ChatDetailResponse;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.dto.ChatMessageResponse;
import com.aikids.care.domain.chat.dto.ChatUpdateRequest;
import com.aikids.care.domain.chat.dto.ChatStreamResponse;
import com.aikids.care.domain.chat.dto.VoiceChatResponse;
import com.aikids.care.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
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

    // 음성 상담 SSE 스트리밍 (POST /api/chats/{chatId}/voices)
    @PostMapping(value = "/{chatId}/voices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamVoiceChat(
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file) {
        return chatService.handleVoiceChatStream(chatId, file)
                .map(chunk -> ServerSentEvent.<ChatStreamResponse>builder()
                        .data(chunk)
                        .build())
                .doOnError(e -> log.error("[VoiceStream] chatId={} stream error", chatId, e))
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<ChatStreamResponse>builder()
                                .data(ChatStreamResponse.ofChunk("음성 상담 처리 중 오류: " + e.getMessage(), ""))
                                .build(),
                        ServerSentEvent.<ChatStreamResponse>builder()
                                .data(ChatStreamResponse.ofFinal())
                                .build()
                ));
    }

    // 동기 음성 상담 (레거시, 필요 시 사용)
    @PostMapping(value = "/{chatId}/voices/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendVoiceMessageSync(@PathVariable Long chatId,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            VoiceChatResponse response = chatService.sendVoiceMessage(chatId, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[VoiceSync] chatId={} failed: {}", chatId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "voice 처리 실패: " + e.getMessage()));
        }
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
