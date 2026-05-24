package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Bean
    public GeminiApiClient geminiApiClient(
            ChatModel chatModel,
            @Autowired(required = false) VectorStore vectorStore) {
        return new GeminiApiClient(chatModel, vectorStore);
    }
}
