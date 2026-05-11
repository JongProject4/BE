package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini 호출을 담당하는 클라이언트.
 */
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

@Configuration
class GeminiClientConfig {
    @Bean
    @ConditionalOnBean(ChatModel.class)
    GeminiApiClient geminiApiClient(ChatModel chatModel) {
        return new GeminiApiClient(chatModel);
    }
}