package com.aikids.care.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatMessageRequest {
    // 부모님이 스마트폰 앱에서 입력해서 보낼 텍스트 내용입니다.
    private String content;
}