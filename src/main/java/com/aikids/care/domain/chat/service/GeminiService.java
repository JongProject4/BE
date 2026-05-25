package com.aikids.care.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
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
            "부모가 아이의 증상을 말하면, 1) 공감하고 안심시킬 것, " +
            "2) 가정에서 할 수 있는 응급 처치법을 안내할 것, " +
            "3) 심각한 경우 즉시 응급실이나 소아과 방문을 권고할 것. " +
            "절대 확정적인 의료 진단을 내리지 마세요.";

    public String askQuestion(String parentMessage, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            log.warn("[Gemini] imageUrl is not supported in askQuestion yet. imageUrl={}", imageUrl);
        }

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

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(URL + apiKey, request, Map.class);
            return extractTextFromBody(response.getBody());
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("[Gemini] API HTTP error status={}, body={}", e.getStatusCode(), responseBody);
            throw new IllegalStateException(
                    "Gemini API " + e.getStatusCode() + ": " + abbreviate(responseBody, 300));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromBody(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalStateException("Gemini API returned empty body");
        }
        if (body.containsKey("error")) {
            throw new IllegalStateException("Gemini API error: " + body.get("error"));
        }

        Object candidatesObj = body.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            Object feedback = body.get("promptFeedback");
            throw new IllegalStateException(
                    "Gemini returned no candidates. promptFeedback=" + feedback);
        }

        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            throw new IllegalStateException("Gemini candidate format is invalid");
        }

        Object contentObj = candidate.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            throw new IllegalStateException("Gemini candidate has no content");
        }

        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            throw new IllegalStateException("Gemini content has no parts");
        }

        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            throw new IllegalStateException("Gemini part format is invalid");
        }

        Object text = part.get("text");
        if (text == null) {
            throw new IllegalStateException("Gemini part has no text");
        }
        return text.toString();
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
