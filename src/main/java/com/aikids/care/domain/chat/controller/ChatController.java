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
import com.aikids.care.global.security.OAuth2Utils;
import com.aikids.care.global.security.OAuth2Utils.AuthInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    @PostMapping
    public ResponseEntity<ChatCreateResponse> createChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody ChatCreateRequest request) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        Long chatId = chatService.createChat(auth.socialId(), auth.socialType(), request);
        return ResponseEntity.ok(new ChatCreateResponse(chatId));
    }

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

    @GetMapping("/rooms/list/{childId}")
    public ResponseEntity<List<Long>> getChatRoomList(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        return ResponseEntity.ok(chatService.getChatRoomList(auth.socialId(), auth.socialType(), childId));
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<Void> updateChatResult(@PathVariable Long chatId, @RequestBody ChatUpdateRequest request) {
        chatService.updateChatResult(chatId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatDetailResponse>> getChatHistory(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChatHistory(chatId));
    }

    @PostMapping("/{chatId}/close")
    public ResponseEntity<Void> closeChat(@PathVariable Long chatId) {
        chatService.closeChat(chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.ok().build();
    }
}
