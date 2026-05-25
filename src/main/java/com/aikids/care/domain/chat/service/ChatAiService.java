package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.AiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAiService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public AiAnalysisResponse analyzeContent(String history) {
        String prompt = """
            당신은 아동 건강 상담 전문가입니다. 다음 대화 내용을 분석하여 질병 카테고리와 위험도를 판별하세요.
            
            [응답 규칙]
            1. 반드시 아래의 JSON 형식으로만 답변하세요.
            2. JSON 외에 다른 설명이나 인사는 절대로 하지 마세요.
            3. 카테고리는 반드시 다음 중 하나여야 합니다: FEVER, COUGH, RASH, DIGESTIVE, ETC
            4. 위험도는 반드시 다음 중 하나여야 합니다: NORMAL, CLINIC_VISIT, HOSPITAL
            
            [응답 형식]
            {"category": "FEVER", "riskLevel": "HOSPITAL"}
            
            [대화 내용]
            %s
            """.formatted(history);

        String response = chatModel.call(prompt);
        // 로그를 찍어서 AI가 실제로 뭐라고 대답했는지 꼭 확인해보세요!
        System.out.println("AI Raw Response: " + response);
        return parseResponse(response);
    }

    private AiAnalysisResponse parseResponse(String raw) {
        try {
            // ```json 이나 ``` 같은 마크다운 제거
            String cleaned = raw.replaceAll("(?s)```json|```", "").trim();
            return objectMapper.readValue(cleaned, AiAnalysisResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 파싱 실패: " + e.getMessage());
        }
    }
}