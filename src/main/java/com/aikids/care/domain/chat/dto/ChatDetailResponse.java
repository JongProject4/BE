package com.aikids.care.domain.chat.dto;

import com.aikids.care.domain.chat.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatDetailResponse {
    private Long id;
    private Role role;           // USER 인지 AI 인지
    private String content;      // 대화 내용
    private String imageUrl;     // 이미지 URL (없으면 null)
    private LocalDateTime createdAt;
}