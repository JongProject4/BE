package com.aikids.care.domain.chat.alarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmIntentExtractor {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final List<String> ALARM_KEYWORDS = List.of(
            "알림", "알려줘", "알려주세요", "알려달라",
            "복용", "복약", "먹여야", "먹이라고", "약 먹",
            "내원", "병원", "예약", "검진", "진료",
            "등록", "설정해줘",
            "시간마다", "일에 한 번", "하루", "매일", "이틀", "사흘",
            "내일", "모레", "다음주", "다음 주"
    );

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public AlarmDraft extract(String userMessage, AlarmDraft previousDraft) {
        if (userMessage == null || userMessage.isBlank()) {
            return AlarmDraft.none();
        }

        boolean hasPendingDraft = previousDraft != null
                && previousDraft.getIntent() != null
                && previousDraft.getIntent() != AlarmIntent.NONE;

        // 진행 중 초안이 있으면 사용자가 슬롯만 짧게 답할 수 있어 키워드 매칭이 안 잡힘 → LLM 직행
        if (!hasPendingDraft && !matchesAlarmKeyword(userMessage)) {
            log.debug("[AlarmIntent] heuristic skipped: '{}'", abbreviate(userMessage));
            return AlarmDraft.none();
        }

        String prompt = AlarmIntentPrompts.extract(userMessage, LocalDateTime.now(KST), previousDraft);
        String response;
        try {
            response = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[AlarmIntent] LLM call failed", e);
            return AlarmDraft.none();
        }
        log.debug("[AlarmIntent] LLM raw: {}", response);

        try {
            String cleaned = response.replaceAll("(?s)```json|```", "").trim();
            AlarmDraft parsed = objectMapper.readValue(cleaned, AlarmDraft.class);
            return parsed == null ? AlarmDraft.none() : parsed;
        } catch (Exception e) {
            log.warn("[AlarmIntent] parse failed, raw='{}'", response, e);
            return AlarmDraft.none();
        }
    }

    private boolean matchesAlarmKeyword(String message) {
        for (String keyword : ALARM_KEYWORDS) {
            if (message.contains(keyword)) return true;
        }
        return false;
    }

    private String abbreviate(String text) {
        if (text.length() <= 60) return text;
        return text.substring(0, 60) + "...";
    }
}
