package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.AiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAiService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public AiAnalysisResponse analyzeContent(String history) {
        String prompt = """
            당신은 아동 건강 상담 전문가입니다. 다음 대화 내용을 분석하여 질병 카테고리(category)와 위험도(riskLevel)를 판별하세요.
            
            [판별 원칙 — 매우 중요]
            1. 위험도는 "부모:" 발화에 보고된 현재 아이 상태를 최우선으로 판단하세요.
            2. "AI:" 발화는 참고만 하세요. AI가 "악화되면", "~하면 응급실", "심해지면" 등으로 한 조건부·예방 안내는
               지금 당장의 위험도가 아닙니다. 이런 문장만으로 EMERGENCY_ROOM을 선택하지 마세요.
            3. EMERGENCY_ROOM은 부모가 지금 겪고 있다고 보고한 위중 증상(의식 변화, 호흡 곤란, 입술·얼굴 청색증,
               경련, 대량 출혈, 심한 탈수 등)이 있을 때만 선택하세요.
            4. 해열제 복용 후 가정 관리, 외래 진료 권고 수준의 증상은 HOME_CARE 또는 CLINIC_VISIT을 우선 고려하세요.
            5. 대화 전체 흐름에서 부모가 말한 사실과 현재 처치 상황을 종합하되, 조건부 안내 문구에 끌려가지 마세요.
            
            [응답 규칙]
            1. 반드시 아래 JSON 형식으로만 답변하세요. JSON 외 설명·인사는 금지합니다.
            2. category는 반드시 다음 중 정확히 하나 (대소문자 구분):
               FEVER, DIGESTIVE, RESPIRATORY, SKIN, TRAUMA, ETC
            3. riskLevel은 반드시 다음 중 정확히 하나 (대소문자 구분):
               HOME_CARE, CLINIC_VISIT, EMERGENCY_ROOM, RE_CONSULT
            4. COUGH, RASH, NORMAL, HOSPITAL 등 위 목록에 없는 값은 사용하지 마세요.
            
            [판별 예시]
            - 부모: 열 38도, 해열제 먹였어요 / AI: 악화 시 응급실 → {"category":"FEVER","riskLevel":"HOME_CARE"}
            - 부모: 기침 3일째, 열 있어요 / AI: 호흡 힘들면 응급실 → {"category":"RESPIRATORY","riskLevel":"CLINIC_VISIT"}
            - 부모: 숨쉬기 힘들어하고 입술이 파래요 → {"category":"RESPIRATORY","riskLevel":"EMERGENCY_ROOM"}
            
            [응답 형식]
            {"category": "FEVER", "riskLevel": "CLINIC_VISIT"}
            
            [대화 내용]
            %s
            """.formatted(history);

        String response = chatModel.call(prompt);
        // 로그를 찍어서 AI가 실제로 뭐라고 대답했는지 꼭 확인해보세요!
        log.debug("[ChatAiService] AI raw response: {}", response);
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