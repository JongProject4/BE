package com.aikids.care.infra.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 텍스트 스트리밍(gemini-2.5-flash 등) + 문장 단위 Gemini-TTS 합성 파이프라인.
 */
@Slf4j
@Component
public class GeminiStreamClient {

    private static final String SHORT_PEDIATRIC_SYSTEM_PROMPT = """
            당신은 야간 소아 응급 상황에서 부모를 안심시키는 친절한 소아과 전문의입니다.
            [중요] 절대로 길게 격식 차려 말하지 마세요. 보호자가 다급한 상황이므로 핵심만 3문장 이내로 짧게 대화하듯 상담해 주세요.
            예시: "어머니, 아이가 갑자기 열이 나서 많이 놀라셨죠? 우선 해열제를 몸무게에 맞춰 먹이시고 미온수로 몸을 가볍게 닦아주세요. 혹시 숨소리가 쌕쌕거리거나 처지나요?"
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String streamModel;
    private final String ttsModel;
    private final String voiceName;
    private final int maxOutputTokens;
    private final boolean logRawSse;

    public GeminiStreamClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${app.gemini.stream-model:gemini-2.5-flash}") String streamModel,
            @Value("${app.gemini.tts-model:gemini-2.5-flash-preview-tts}") String ttsModel,
            @Value("${app.gemini.voice-name:Kore}") String voiceName,
            @Value("${app.gemini.max-output-tokens:256}") int maxOutputTokens,
            @Value("${app.gemini.log-raw-sse:true}") boolean logRawSse) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.streamModel = streamModel;
        this.ttsModel = ttsModel;
        this.voiceName = voiceName;
        this.maxOutputTokens = maxOutputTokens;
        this.logRawSse = logRawSse;
    }

    /**
     * 텍스트 전용 고속 스트리밍 (TEXT 모달리티만 요청).
     */
    public Flux<GeminiStreamChunk> streamTextOnly(String userMessage) {
        log.info("[GeminiStream] text-only model={}, userMessage='{}'", streamModel, abbreviate(userMessage, 120));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SHORT_PEDIATRIC_SYSTEM_PROMPT))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userMessage))
                        )
                ),
                "generationConfig", textOnlyGenerationConfig()
        );

        return streamViaSse(streamModel, body);
    }

    /**
     * Gemini-TTS: 문장 단위 텍스트를 생성형 음성(Base64)으로 변환.
     */
    public Mono<String> synthesizeAudio(String text) {
        if (text == null || text.isBlank()) {
            return Mono.empty();
        }

        Map<String, Object> speechConfig = new HashMap<>();
        speechConfig.put("voiceConfig", Map.of(
                "prebuiltVoiceConfig", Map.of("voiceName", voiceName)
        ));
        speechConfig.put("languageCode", "ko-KR");

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", text))
                        )
                ),
                "generationConfig", Map.of(
                        "responseModalities", List.of("AUDIO"),
                        "speechConfig", speechConfig
                )
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(ttsModel))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("(empty body)")
                        .flatMap(errorBody -> {
                            log.error("[Gemini-TTS] API error status={}, model={}, body={}",
                                    response.statusCode(), ttsModel, errorBody);
                            return Mono.error(new IllegalStateException(
                                    "Gemini TTS API " + response.statusCode() + ": " + errorBody));
                        }))
                .bodyToMono(JsonNode.class)
                .mapNotNull(this::extractAudioFromResponse)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(audio -> log.debug("[Gemini-TTS] synthesized {} chars -> {} bytes (base64)",
                        text.length(), audio != null ? audio.length() : 0))
                .onErrorResume(error -> {
                    log.error("[Gemini-TTS] 음성 합성 실패. 텍스트: {}", abbreviate(text, 80), error);
                    return Mono.empty();
                });
    }

    private Flux<GeminiStreamChunk> streamViaSse(String model, Map<String, Object> body) {
        AtomicInteger sseEventCount = new AtomicInteger(0);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("(empty body)")
                        .flatMap(errorBody -> {
                            log.error("[GeminiStream] API error status={}, model={}, body={}",
                                    response.statusCode(), model, errorBody);
                            return Mono.error(new IllegalStateException(
                                    "Gemini stream API " + response.statusCode() + ": " + errorBody));
                        }))
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> {
                    int index = sseEventCount.incrementAndGet();
                    if (logRawSse) {
                        log.info("[GeminiStream] SSE event #{} raw={}", index, abbreviate(event.data(), 800));
                    }
                })
                .flatMap(event -> parseSsePayload(event.data()))
                .filter(GeminiStreamChunk::hasContent)
                .doOnComplete(() -> log.info("[GeminiStream] SSE HTTP ended, rawEvents={}, model={}",
                        sseEventCount.get(), model))
                .doOnError(error -> log.error("[GeminiStream] stream failed model={}", model, error));
    }

    private Map<String, Object> textOnlyGenerationConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxOutputTokens", maxOutputTokens);
        config.put("temperature", 0.7);
        // gemini-2.5-flash: thinking 토큰이 maxOutputTokens 예산을 잡아먹으면 본문이 수 토큰만 나옴
        config.put("thinkingConfig", Map.of("thinkingBudget", 0));
        return config;
    }

    private Flux<GeminiStreamChunk> parseSsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Flux.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root.has("error")) {
                String message = root.path("error").path("message").asText("Unknown Gemini error");
                log.error("[GeminiStream] error in SSE payload: {}", root);
                return Flux.error(new IllegalStateException("Gemini SSE error: " + message));
            }

            if (root.isArray()) {
                List<GeminiStreamChunk> chunks = new ArrayList<>();
                for (JsonNode element : root) {
                    logStreamMetadata(element);
                    chunks.addAll(extractChunks(element));
                }
                log.info("[GeminiStream] parsed JSON array root, textChunks={}", chunks.size());
                return Flux.fromIterable(chunks);
            }

            logStreamMetadata(root);
            List<GeminiStreamChunk> chunks = extractChunks(root);
            if (chunks.isEmpty()) {
                log.info("[GeminiStream] SSE payload had no extractable text (metadata-only?)");
            }
            return Flux.fromIterable(chunks);
        } catch (Exception e) {
            log.error("[GeminiStream] Failed to parse SSE payload: {}", abbreviate(payload, 200), e);
            return Flux.error(e);
        }
    }

    private List<GeminiStreamChunk> extractChunks(JsonNode root) {
        List<GeminiStreamChunk> chunks = new ArrayList<>();
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray()) {
            return chunks;
        }
        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                String finishReason = candidate.path("finishReason").asText("");
                if (!finishReason.isEmpty()) {
                    log.info("[GeminiStream] candidate without parts, finishReason={}", finishReason);
                }
                continue;
            }
            StringBuilder text = new StringBuilder();
            String audio = "";
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    text.append(part.get("text").asText(""));
                }
                JsonNode inlineData = part.path("inlineData");
                if (inlineData.hasNonNull("data")) {
                    audio = inlineData.get("data").asText("");
                }
            }
            if (!text.isEmpty() || !audio.isBlank()) {
                chunks.add(new GeminiStreamChunk(text.toString(), audio));
            }
        }
        return chunks;
    }

    private void logStreamMetadata(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray()) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            JsonNode candidate = candidates.get(i);
            String finishReason = candidate.path("finishReason").asText("");
            if (!finishReason.isEmpty()) {
                if ("MAX_TOKENS".equals(finishReason)) {
                    JsonNode usage = root.path("usageMetadata");
                    log.warn("[GeminiStream] candidate[{}] finishReason=MAX_TOKENS — output truncated. "
                                    + "candidatesTokenCount={}, thoughtsTokenCount={}, totalTokenCount={}, maxOutputTokens={}",
                            i,
                            usage.path("candidatesTokenCount").asInt(-1),
                            usage.path("thoughtsTokenCount").asInt(-1),
                            usage.path("totalTokenCount").asInt(-1),
                            maxOutputTokens);
                } else {
                    log.info("[GeminiStream] candidate[{}] finishReason={}", i, finishReason);
                }
            }
        }
        JsonNode promptFeedback = root.path("promptFeedback");
        if (!promptFeedback.isMissingNode() && !promptFeedback.isEmpty()) {
            log.info("[GeminiStream] promptFeedback={}", promptFeedback);
        }
    }

    private String extractAudioFromResponse(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return null;
        }
        for (JsonNode part : parts) {
            JsonNode inlineData = part.path("inlineData");
            if (inlineData.hasNonNull("data")) {
                return inlineData.get("data").asText("");
            }
        }
        return null;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
