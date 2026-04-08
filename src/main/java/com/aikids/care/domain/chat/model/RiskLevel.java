package com.aikids.care.domain.chat.model;

public enum RiskLevel {
    HOME_CARE,      // 가정 처치
    CLINIC_VISIT,   // 외래 방문 권고
    EMERGENCY,      // 즉시 응급실
    RE_CONSULT,     // 재상담
    PENDING         // 판독 중
}