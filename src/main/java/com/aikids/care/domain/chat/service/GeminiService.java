package com.aikids.care.domain.chat.service;

import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String MODEL = "gemini-2.5-flash";
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        + MODEL + ":generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "당신은 친절하고 전문적인 '소아 응급 상담 AI 보조'입니다. " +
            "부모가 아이의 증상을 말하면, 1) 공감하고 안심시킬 것, " +
            "2) 가정에서 할 수 있는 응급 처치법을 안내할 것, " +
            "3) 심각한 경우 즉시 응급실이나 소아과 방문을 권고할 것. " +
            "절대 확정적인 의료 진단을 내리지 마세요.";

    private static final String SUMMARY_PROMPT =
            "다음은 부모와 소아 응급 상담 AI 간의 대화입니다. " +
            "아이의 증상, 상태, 조치 내용을 중심으로 3문장 이내로 요약해주세요.";

    public String askQuestion(String userMessage, List<Map<String, String>> history) {
        List<Map<String, Object>> contents = buildContents(history, userMessage);

        Map<String, Object> body = Map.of(
            "system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
            "contents", contents
        );

        return callGemini(body);
    }

    public String summarize(List<Map<String, String>> history) {
        String conversationText = history.stream()
                .map(m -> m.get("role") + ": " + m.get("content"))
                .reduce("", (a, b) -> a + "\n" + b);

        Map<String, Object> body = Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            ),
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", SUMMARY_PROMPT + "\n\n" + conversationText)))
            )
        );

        return callGemini(body);
    }

    private List<Map<String, Object>> buildContents(List<Map<String, String>> history, String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> message : history) {
            contents.add(Map.of(
                "role", message.get("role"),
                "parts", List.of(Map.of("text", message.get("content")))
            ));
        }
        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userMessage))
        ));
        return contents;
    }

    private String callGemini(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(URL + apiKey, request, Map.class);
        } catch (HttpServerErrorException e) {
            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new CustomException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            List<Map> candidates = (List<Map>) response.getBody().get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
