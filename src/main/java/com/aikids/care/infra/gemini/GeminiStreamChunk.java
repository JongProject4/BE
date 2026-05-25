package com.aikids.care.infra.gemini;

/**
 * Gemini 스트림 청크에서 추출한 텍스트·오디오 조각.
 */
public record GeminiStreamChunk(String text, String audioBase64) {

    public static GeminiStreamChunk empty() {
        return new GeminiStreamChunk("", "");
    }

    public boolean hasContent() {
        return (text != null && !text.isBlank()) || (audioBase64 != null && !audioBase64.isBlank());
    }
}
