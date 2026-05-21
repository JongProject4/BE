package com.aikids.care.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //에러 코드 목록 enum
    // 추후 에러 코드 수정시 해당 클래스만 변경
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Common
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    // Child
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 아이 프로필을 찾을 수 없습니다."),

    // Chat
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상담 세션을 찾을 수 없습니다."),

    // Health Log
    HEALTH_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 헬스 로그를 찾을 수 없습니다."),

    // Medication Alarm
    MEDICATION_ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 복약 알림을 찾을 수 없습니다."),

    // Hospital Alarm
    HOSPITAL_ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 내원 알림을 찾을 수 없습니다."),

    // AI
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}