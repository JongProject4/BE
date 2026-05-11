package com.aikids.care.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {
    private String answer; // AI의 답변 내용
}
