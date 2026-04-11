package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient {

    private final ChatModel chatModel;

    public GeminiApiClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askToGemini(String parentMessage) {
        System.out.println("부모님 질문: " + parentMessage);

        String aiResponse = chatModel.call(parentMessage);

        System.out.println("제미나이 답변: " + aiResponse);
        return aiResponse;
    }
}