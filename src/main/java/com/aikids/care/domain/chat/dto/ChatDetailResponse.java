package com.aikids.care.domain.chat.dto;

import com.aikids.care.domain.chat.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatDetailResponse {
    private Role role;           // USER인지 AI인지
    private String content;      // 대화 내용
    private LocalDateTime time;  // 보낸 시간
}