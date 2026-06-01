package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatStreamResponse;
import com.aikids.care.infra.gemini.GeminiStreamClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gemini 텍스트 스트리밍(읽기 전용)과 TTS(별도 큐·순차 워커)를 분리한다.
 * TTS 대기 때문에 Gemini SSE 수신이 멈추는 문제를 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceChatStreamPipeline {

    private static final Duration TTS_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration STREAM_WATCHDOG = Duration.ofSeconds(90);

    private final GeminiStreamClient geminiStreamClient;
    private final ChatMessagePersistence chatMessagePersistence;
    private final GeminiService geminiService;

    @Value("${app.voice-stream.tts-enabled:true}")
    private boolean ttsEnabled;

    @Value("${app.voice-stream.use-sync-llm:false}")
    private boolean useSyncLlm;

    @Value("${app.voice-stream.tts-split-on-comma:true}")
    private boolean ttsSplitOnComma;

    @Value("${app.voice-stream.min-tts-chars:8}")
    private int minTtsChars;

    public Flux<ChatStreamResponse> stream(Long chatId, String transcript,
                                           List<Map<String, String>> history, String interimSummary) {
        if (useSyncLlm) {
            log.warn("[VoiceStream] use-sync-llm=true, bypassing streamGenerateContent chatId={}", chatId);
            return streamViaSyncLlm(chatId, transcript);
        }
        return Flux.create(sink -> {
            if (!ttsEnabled) {
                log.warn("[VoiceStream] TTS disabled (app.voice-stream.tts-enabled=false), text-only mode chatId={}",
                        chatId);
            }
            AtomicReference<String> mergedText = new AtomicReference<>("");
            StringBuilder sentenceBuffer = new StringBuilder();
            AtomicBoolean isFirstTextChunk = new AtomicBoolean(true);
            AtomicBoolean geminiCompleted = new AtomicBoolean(false);
            AtomicBoolean ttsWorkerBusy = new AtomicBoolean(false);
            AtomicBoolean streamFinalized = new AtomicBoolean(false);
            AtomicInteger geminiChunkCount = new AtomicInteger(0);
            Queue<String> ttsQueue = new ConcurrentLinkedQueue<>();
            AtomicReference<Disposable> watchdogRef = new AtomicReference<>();

            Runnable finalizeStream = () -> {
                if (!streamFinalized.compareAndSet(false, true)) {
                    return;
                }
                Disposable watchdog = watchdogRef.get();
                if (watchdog != null && !watchdog.isDisposed()) {
                    watchdog.dispose();
                }

                String aiContent = mergedText.get();
                log.info("[VoiceStream] chatId={}, geminiChunks={}, saving AI reply length={}",
                        chatId, geminiChunkCount.get(), aiContent.length());

                Mono.fromRunnable(() -> chatMessagePersistence.saveAiReply(chatId, aiContent))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                unused -> {
                                },
                                error -> {
                                    log.error("[VoiceStream] DB save failed chatId={}", chatId, error);
                                    sink.next(ChatStreamResponse.ofFinal());
                                    sink.complete();
                                },
                                () -> {
                                    sink.next(ChatStreamResponse.ofFinal());
                                    sink.complete();
                                }
                        );
            };

            Runnable tryCompleteStream = () -> {
                if (streamFinalized.get()) {
                    return;
                }
                if (!geminiCompleted.get()) {
                    log.debug("[VoiceStream] chatId={} waiting for Gemini completion", chatId);
                    return;
                }
                if (ttsWorkerBusy.get()) {
                    log.debug("[VoiceStream] chatId={} waiting for TTS worker", chatId);
                    return;
                }
                if (!ttsQueue.isEmpty()) {
                    log.debug("[VoiceStream] chatId={} TTS queue size={}", chatId, ttsQueue.size());
                    return;
                }
                finalizeStream.run();
            };

            AtomicReference<Runnable> drainTtsQueueRef = new AtomicReference<>();
            Runnable drainTtsQueue = () -> {
                if (streamFinalized.get() || ttsWorkerBusy.get()) {
                    return;
                }
                String sentence = ttsQueue.poll();
                if (sentence == null) {
                    tryCompleteStream.run();
                    return;
                }

                ttsWorkerBusy.set(true);
                log.info("[VoiceStream] TTS start chatId={}, sentence='{}'", chatId, abbreviate(sentence, 80));

                geminiStreamClient.synthesizeAudio(sentence)
                        .timeout(TTS_TIMEOUT)
                        .onErrorResume(error -> {
                            log.error("[VoiceStream] TTS failed chatId={}, sentence='{}'",
                                    chatId, abbreviate(sentence, 80), error);
                            return Mono.empty();
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                audio -> {
                                    if (audio != null && !audio.isBlank()) {
                                        sink.next(ChatStreamResponse.ofChunk("", audio));
                                    }
                                    ttsWorkerBusy.set(false);
                                    drainTtsQueueRef.get().run();
                                },
                                error -> {
                                    log.error("[VoiceStream] TTS subscribe error chatId={}", chatId, error);
                                    ttsWorkerBusy.set(false);
                                    drainTtsQueueRef.get().run();
                                },
                                () -> {
                                    ttsWorkerBusy.set(false);
                                    drainTtsQueueRef.get().run();
                                }
                        );
            };
            drainTtsQueueRef.set(drainTtsQueue);

            Runnable startWatchdog = () -> watchdogRef.set(
                    Mono.delay(STREAM_WATCHDOG)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    ignored -> {
                                        if (streamFinalized.get()) {
                                            return;
                                        }
                                        log.warn("[VoiceStream] watchdog timeout chatId={}, queueSize={}, ttsBusy={}",
                                                chatId, ttsQueue.size(), ttsWorkerBusy.get());
                                        ttsQueue.clear();
                                        ttsWorkerBusy.set(false);
                                        tryCompleteStream.run();
                                    },
                                    error -> log.error("[VoiceStream] watchdog error chatId={}", chatId, error)
                            )
            );

            Disposable geminiSubscription = geminiStreamClient.streamTextOnly(transcript, history, interimSummary)
                    .doOnNext(chunk -> log.debug("[VoiceStream] Gemini chunk chatId={}, textLength={}",
                            chatId, chunk.text() != null ? chunk.text().length() : 0))
                    .subscribe(
                            chunk -> {
                                String incoming = chunk.text() != null ? chunk.text() : "";
                                if (incoming.isBlank()) {
                                    return;
                                }

                                geminiChunkCount.incrementAndGet();
                                String previous = mergedText.get();
                                String merged = mergeStreamText(previous, incoming);
                                mergedText.set(merged);

                                String delta = extractDelta(previous, merged, incoming);
                                sentenceBuffer.append(delta);

                                if (!delta.isBlank()) {
                                    if (isFirstTextChunk.getAndSet(false)) {
                                        sink.next(ChatStreamResponse.ofFirstChunk(transcript, delta, ""));
                                    } else {
                                        sink.next(ChatStreamResponse.ofChunk(delta, ""));
                                    }
                                }

                                if (ttsEnabled) {
                                    enqueueCompletedTts(sentenceBuffer, ttsQueue, chatId, drainTtsQueue);
                                }
                            },
                            error -> {
                                log.error("[VoiceStream] Gemini stream error chatId={}", chatId, error);
                                sink.error(error);
                            },
                            () -> {
                                geminiCompleted.set(true);
                                log.info("[VoiceStream] Gemini stream completed chatId={}, chunks={}, mergedLength={}",
                                        chatId, geminiChunkCount.get(), mergedText.get().length());

                                if (ttsEnabled) {
                                    String remainder = sentenceBuffer.toString().trim();
                                    if (!remainder.isEmpty()) {
                                        ttsQueue.offer(remainder);
                                        log.info("[VoiceStream] chatId={} enqueue remainder TTS length={}",
                                                chatId, remainder.length());
                                    }
                                    startWatchdog.run();
                                    drainTtsQueue.run();
                                }
                                tryCompleteStream.run();
                            }
                    );

            sink.onDispose(() -> {
                geminiSubscription.dispose();
                Disposable watchdog = watchdogRef.get();
                if (watchdog != null) {
                    watchdog.dispose();
                }
                if (!streamFinalized.get()) {
                    log.warn("[VoiceStream] client disconnected chatId={}, forcing finalize", chatId);
                    geminiCompleted.set(true);
                    ttsQueue.clear();
                    ttsWorkerBusy.set(false);
                    tryCompleteStream.run();
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * /messages·/voices/sync와 동일한 v1 generateContent — 스트림 API 문제인지 A/B 비교용.
     */
    private Flux<ChatStreamResponse> streamViaSyncLlm(Long chatId, String transcript) {
        return Mono.fromCallable(() -> geminiService.askQuestion(chatId, transcript, null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(aiText -> Flux.concat(
                        Flux.just(ChatStreamResponse.ofFirstChunk(transcript, aiText, "")),
                        Mono.fromRunnable(() -> chatMessagePersistence.saveAiReply(chatId, aiText))
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(Mono.just(ChatStreamResponse.ofFinal()))
                ))
                .doOnNext(chunk -> log.info("[VoiceStream] sync-llm chunk chatId={}, textLength={}",
                        chatId, chunk.text() != null ? chunk.text().length() : 0))
                .doOnError(error -> log.error("[VoiceStream] sync-llm failed chatId={}", chatId, error));
    }

    private String mergeStreamText(String accumulated, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return accumulated != null ? accumulated : "";
        }
        if (accumulated == null || accumulated.isEmpty()) {
            return incoming;
        }
        if (incoming.startsWith(accumulated)) {
            return incoming;
        }
        if (accumulated.endsWith(incoming)) {
            return accumulated;
        }
        return accumulated + incoming;
    }

    private String extractDelta(String previous, String merged, String incoming) {
        if (merged.length() > previous.length()) {
            return merged.substring(previous.length());
        }
        if (incoming.startsWith(previous) && incoming.length() > previous.length()) {
            return incoming.substring(previous.length());
        }
        return incoming;
    }

    private void enqueueCompletedTts(
            StringBuilder buffer,
            Queue<String> ttsQueue,
            Long chatId,
            Runnable drainTtsQueue
    ) {
        List<String> completed = drainCompletedSentences(buffer);
        if (completed.isEmpty()) {
            return;
        }
        ttsQueue.addAll(completed);
        log.info("[VoiceStream] TTS queued {} segment(s) during stream chatId={}, queueSize={}",
                completed.size(), chatId, ttsQueue.size());
        drainTtsQueue.run();
    }

    /**
     * 문장 종료 부호에서 TTS용 세그먼트를 잘라낸다.
     * ASCII(.?!)뿐 아니라 전각(。？！)、줄임표(…·...)·쉼표(，,)도 처리한다.
     */
    private List<String> drainCompletedSentences(StringBuilder buffer) {
        List<String> segments = new ArrayList<>();
        String content = buffer.toString();
        int lastEnd = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!isTtsSplitPoint(c, content, i)) {
                continue;
            }

            String segment = content.substring(lastEnd, i + 1).trim();
            if (segment.length() >= minTtsChars) {
                segments.add(segment);
            }
            lastEnd = i + 1;
        }

        buffer.setLength(0);
        if (lastEnd < content.length()) {
            buffer.append(content.substring(lastEnd));
        }
        return segments;
    }

    private boolean isTtsSplitPoint(char c, String content, int index) {
        if (c == '\n' || c == '?' || c == '!' || c == '\uFF1F' || c == '\uFF01') {
            return true;
        }
        if (c == '\u2026') {
            return true;
        }
        if (c == '.' || c == '\uFF0E') {
            // "..." 중간 점에서는 자르지 않고 마지막 점에서만 분리
            if (index + 1 < content.length() && content.charAt(index + 1) == '.') {
                return false;
            }
            return true;
        }
        if (ttsSplitOnComma && (c == ',' || c == '\uFF0C')) {
            return true;
        }
        return false;
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
