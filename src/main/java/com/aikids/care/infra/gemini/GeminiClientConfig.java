package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Bean
    @ConditionalOnBean(ChatModel.class)
    public GeminiApiClient geminiApiClient(ChatModel chatModel) {
        return new GeminiApiClient(chatModel);
    }
}
