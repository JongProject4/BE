package com.aikids.care.domain.chat.alarm;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AlarmIntentPrompts {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private AlarmIntentPrompts() {
    }

    public static String extract(String userMessage, LocalDateTime now, AlarmDraft previousDraft) {
        String previousJsonHint = previousDraft == null || previousDraft.getIntent() == AlarmIntent.NONE
                ? "없음"
                : """
                    intent=%s, medicineName=%s, dosage=%s, intervalHour=%s, hospitalName=%s, visitDate=%s, memo=%s
                    """.formatted(
                        previousDraft.getIntent(),
                        nullToDash(previousDraft.getMedicineName()),
                        nullToDash(previousDraft.getDosage()),
                        previousDraft.getIntervalHour() == null ? "-" : previousDraft.getIntervalHour().toString(),
                        nullToDash(previousDraft.getHospitalName()),
                        previousDraft.getVisitDate() == null ? "-" : previousDraft.getVisitDate().format(ISO),
                        nullToDash(previousDraft.getMemo())
                ).strip();

        return """
                당신은 소아 건강 상담 앱의 알람 등록 보조입니다.
                사용자의 메시지에서 '복약 알림' 또는 '내원(병원 방문) 알림' 등록 의도와 슬롯을 추출하세요.
                알람 등록 의도가 전혀 없으면 intent를 "NONE"으로 두세요.

                [응답 규칙]
                1. 반드시 아래의 JSON 형식으로만 답하세요. JSON 외 텍스트·마크다운·주석 절대 금지.
                2. intent 는 다음 중 하나: "MEDICATION", "HOSPITAL", "NONE"
                3. 추출이 불확실한 필드는 null 로 두세요. 추측하지 마세요.
                4. intervalHour 는 정수(시간 단위). "하루 3번" => 8, "12시간마다" => 12, "이틀에 한 번" => 48.
                5. visitDate 는 ISO 8601 LOCAL DATETIME ("yyyy-MM-ddTHH:mm:ss"). 시간 정보가 없으면 09:00:00 으로 채우세요.
                6. 모든 시각은 한국 시간(KST) 기준입니다. 상대 시간 표현("내일", "다음주" 등)은 아래 '현재 시각'(KST)을 기준으로 변환하세요.
                7. dosage 는 사용자가 말한 양 그대로 ("5ml", "한 알", "반 스푼" 등). 단위가 없으면 null.
                8. medicineName / hospitalName 은 고유명사 그대로. 일반 표현("그 약")은 null.
                9. 이전 알람 초안이 있고 사용자가 그 슬롯을 채우는 발화면, 채워진 슬롯만 출력하고 나머지는 null.

                [응답 형식]
                {
                  "intent": "MEDICATION" | "HOSPITAL" | "NONE",
                  "medicineName": string | null,
                  "dosage": string | null,
                  "intervalHour": number | null,
                  "hospitalName": string | null,
                  "visitDate": string | null,
                  "memo": string | null
                }

                [현재 시각]
                %s

                [이전 알람 초안]
                %s

                [사용자 메시지]
                %s
                """.formatted(now.format(ISO), previousJsonHint, userMessage);
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
