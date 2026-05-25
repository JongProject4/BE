package com.aikids.care.domain.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String MODEL = "gemini-2.5-flash";
    private static final String URL = "https://generativelanguage.googleapis.com/v1/models/"
        + MODEL + ":generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "당신은 친절하고 전문적인 '소아 응급 상담 AI 보조'입니다. " +
            "부모가 아이의 증상을 말하면, " +
            "가정에서 할 수 있는 응급 처치법을 안내할 것, " +
            "심각한 경우 즉시 응급실이나 소아과 방문을 권고할 것. " +
            "절대 확정적인 의료 진단을 내리지 마세요." +
            "마크다운 기호를 사용하지 않고 번호만 달아서 가독성을 높여주세요"+
            "너무 길게 답변하지 말고 핵심만 전달하세요";

    public String askQuestion(String parentMessage, String imageUrl) {
        String fullMessage = SYSTEM_PROMPT + "\n\n부모 질문: " + parentMessage;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", fullMessage)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            URL + apiKey, request, Map.class
        );

        List<Map> candidates = (List<Map>) response.getBody().get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
