package com.aikids.care.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamResponse(
        String transcript,
        String text,
        String audio,
        boolean isFinal
) {
    public static ChatStreamResponse ofChunk(String text, String audio) {
        return new ChatStreamResponse(null, text, audio, false);
    }

    public static ChatStreamResponse ofFirstChunk(String transcript, String text, String audio) {
        return new ChatStreamResponse(transcript, text, audio, false);
    }

    public static ChatStreamResponse ofFinal() {
        return new ChatStreamResponse(null, null, null, true);
    }
}
