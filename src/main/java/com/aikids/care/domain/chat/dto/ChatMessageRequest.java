package com.aikids.care.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    private String content;
    private String imageUrl; // 스키마에 추가된 이미지 경로 반영
}