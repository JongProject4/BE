package com.aikids.care.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceChatResponse {
    private String transcript;
    private String answer;
}
