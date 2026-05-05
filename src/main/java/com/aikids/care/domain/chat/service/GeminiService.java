package com.aikids.care.domain.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final ChatClient chatClient;

    // 스프링 최신 버전(Spring AI)에서는 ChatClient.Builder를 주입받아 사용합니다.
    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                // 🌟 시스템 프롬프트: AI에게 "너는 소아 응급 의사야"라고 페르소나를 부여합니다.
                .defaultSystem("당신은 친절하고 전문적인 '소아 응급 상담 AI 보조'입니다. " +
                        "부모가 아이의 증상을 말하면, 1) 공감하고 안심시킬 것, " +
                        "2) 가정에서 할 수 있는 응급 처치법을 안내할 것, " +
                        "3) 심각한 경우 즉시 응급실이나 소아과 방문을 권고할 것. " +
                        "절대 확정적인 의료 진단을 내리지 마세요.")
                .build();
    }

    // 텍스트 질문을 던지고 답변을 받아오는 메서드
    public String askQuestion(String parentMessage, String imageUrl) {
        return chatClient.prompt()
                .user(parentMessage)
                .call()
                .content(); // AI가 대답한 텍스트만 쏙 뽑아옵니다.
    }

}