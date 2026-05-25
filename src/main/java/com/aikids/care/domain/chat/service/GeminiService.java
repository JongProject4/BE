package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
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
    private final ChildRepository childRepository;

    public GeminiService(ChildRepository childRepository) {
        this.childRepository = childRepository;
    }
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
            "너무 길게 답변하지 말고 핵심만 전달하세요"+
                    "제공되는 [아이의 기본 정보](병력, 알러지)를 '반드시' 참고하여 답변을 도출하세요. " +
                    "만약 아이의 병력이나 알러지에 위험한 성분이나 약물이 있다면 답변에서 강력히 경고하세요. ";

    public String askQuestion(Long id, String parentMessage, String imageUrl) {

        // Child.java의 @Id 필드명이 id이므로 예외 메시지도 명확하게 매핑
        Child child = childRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이 정보를 찾을 수 없습니다. ID: " + id));

        // child.getId()가 정상적인 게터(Getter) 이름이 됩니다.
        String medicalHistory = (child.getMedicalHistory() == null || child.getMedicalHistory().isBlank())
                ? "없음" : child.getMedicalHistory();

        String allergies = (child.getAllergies() == null || child.getAllergies().isBlank())
                ? "없음" : child.getAllergies();
        String fullMessage = """
            %s
            
            [참고 - 아이의 기본 정보]
            - 과거 병력: %s
            - 알러지 정보: %s
            
            [부모 질문]
            %s
            """.formatted(SYSTEM_PROMPT, medicalHistory, allergies, parentMessage);

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
