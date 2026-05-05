package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Spring AI가 {@link ChatModel} 빈을 만들 때만 등록된다.
 * {@code spring.ai.model.chat: none} 인 경우 빈이 없으므로 앱 기동은 계속된다.
 */
@Component
@ConditionalOnBean(ChatModel.class)
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