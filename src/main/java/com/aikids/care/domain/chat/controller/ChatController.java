package com.aikids.care.domain.chat.controller;

import com.aikids.care.domain.chat.dto.*;
import com.aikids.care.domain.chat.service.ChatService;
import com.aikids.care.domain.chat.service.VoiceConcurrencyLimiter;
import com.aikids.care.global.error.ErrorCode;
import com.aikids.care.global.security.OAuth2Utils;
import com.aikids.care.global.security.OAuth2Utils.AuthInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final VoiceConcurrencyLimiter voiceLimiter;

    @PostMapping
    public ResponseEntity<ChatCreateResponse> createChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody ChatCreateRequest request) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        Long chatId = chatService.createChat(auth.socialId(), auth.socialType(), request);
        return ResponseEntity.ok(new ChatCreateResponse(chatId));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId,
            @RequestBody ChatMessageRequest request) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        String aiAnswer = chatService.sendMessage(chatId, auth.socialId(), auth.socialType(), request);
        return ResponseEntity.ok(new ChatMessageResponse(aiAnswer));
    }

    // 음성 상담 SSE 스트리밍 (POST /api/chats/{chatId}/voices)
    @PostMapping(value = "/{chatId}/voices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamVoiceChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file) {
        // 동시 음성 처리 한도 초과 시 즉시 안내 메시지 후 종료 (서버 보호용)
        if (!voiceLimiter.tryAcquire()) {
            log.warn("[VoiceStream] limiter rejected chatId={}, availableSlots={}", chatId, voiceLimiter.availableSlots());
            return Flux.just(
                    ServerSentEvent.<ChatStreamResponse>builder()
                            .data(ChatStreamResponse.ofChunk(ErrorCode.VOICE_BUSY.getMessage(), ""))
                            .build(),
                    ServerSentEvent.<ChatStreamResponse>builder()
                            .data(ChatStreamResponse.ofFinal())
                            .build()
            );
        }
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        return chatService.handleVoiceChatStream(chatId, auth.socialId(), auth.socialType(), file)
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
                ))
                .doFinally(signal -> voiceLimiter.release());
    }

    // 동기 음성 상담 (레거시, 필요 시 사용)
    @PostMapping(value = "/{chatId}/voices/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendVoiceMessageSync(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file) {
        if (!voiceLimiter.tryAcquire()) {
            log.warn("[VoiceSync] limiter rejected chatId={}, availableSlots={}", chatId, voiceLimiter.availableSlots());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", ErrorCode.VOICE_BUSY.getMessage()));
        }
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        try {
            VoiceChatResponse response = chatService.sendVoiceMessage(chatId, auth.socialId(), auth.socialType(), file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[VoiceSync] chatId={} failed: {}", chatId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "음성 상담 처리에 실패했습니다."));
        } finally {
            voiceLimiter.release();
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
    public ResponseEntity<Void> updateChatResult(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId,
            @RequestBody ChatUpdateRequest request) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        chatService.updateChatResult(chatId, auth.socialId(), auth.socialType(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatDetailResponse>> getChatHistory(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        return ResponseEntity.ok(chatService.getChatHistory(chatId, auth.socialId(), auth.socialType()));
    }

    @PostMapping("/{chatId}/close")
    public ResponseEntity<Void> closeChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        chatService.closeChat(chatId, auth.socialId(), auth.socialType());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        chatService.deleteChat(chatId, auth.socialId(), auth.socialType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzeChat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long chatId) {
        AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
        AiAnalysisResponse response = chatService.analyzeChatAndSave(chatId, auth.socialId(), auth.socialType());
        return ResponseEntity.ok(response);
    }
}
