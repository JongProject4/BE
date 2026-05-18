package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI가 {@link ChatModel} 빈을 만들 때만 등록된다.
 * {@code spring.ai.model.chat: none} 인 경우 빈이 없으므로 앱 기동은 계속된다.
 */
@Component
@ConditionalOnBean(ChatModel.class)
public class GeminiApiClient {

    private static final String SYSTEM_PROMPT =
            "당신은 소아 건강 전문 AI 어시스턴트 '지미나이'입니다. " +
            "부모님의 질문에 친절하고 간결하게 답변하세요. " +
            "의학적 판단이 필요한 경우 반드시 소아과 전문의 상담을 권유하세요. " +
            "답변은 한국어로 작성하고 200단어 이내로 요약하세요.";

    private final ChatModel chatModel;

    public GeminiApiClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askToGemini(String parentMessage) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(parentMessage)
        ));
        String aiResponse = chatModel.call(prompt).getResult().getOutput().getText();
        return aiResponse;
    }
}