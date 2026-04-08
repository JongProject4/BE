package com.aikids.care.infra.gemini;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component // "스프링부트야, 이 객체를 네가 관리해 줘!" 라는 명찰
public class GeminiApiClient {

    private final ChatModel chatModel;

    // Spring AI가 알아서 제미나이 통신 장비(ChatModel)를 연결해 줍니다.
    public GeminiApiClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 주방장(Service)이 "이 메시지 제미나이한테 물어봐!" 하고 시킬 때 쓸 메서드
    public String askToGemini(String parentMessage) {
        System.out.println("부모님 질문: " + parentMessage);

        // 놀랍게도 이 단 한 줄로 제미나이에게 질문을 던지고 답변을 받아옵니다!
        String aiResponse = chatModel.call(parentMessage);

        System.out.println("제미나이 답변: " + aiResponse);
        return aiResponse;
    }
}