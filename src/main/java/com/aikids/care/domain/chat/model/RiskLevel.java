package com.aikids.care.domain.chat.model;

public enum RiskLevel {
    HOME_CARE,      // 가정 처치
    CLINIC_VISIT,   // 외래 방문 권고
    EMERGENCY_ROOM, // 즉시 응급실
    RE_CONSULT,     // 재상담
    ANALYZING       // 🌟 판독 중 (초기 상태용)
}