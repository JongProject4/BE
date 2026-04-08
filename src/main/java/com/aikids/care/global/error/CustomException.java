package com.aikids.care.global.error;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    // 예외 발생 시, ErrorCode 내부의 상태코드와 메시지를
    // 클라이언트에 돌려줌
    // 직접 만든 예외가 담기는 곳
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}