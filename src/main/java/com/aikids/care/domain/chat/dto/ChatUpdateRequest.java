package com.aikids.care.domain.chat.dto;

import com.aikids.care.domain.chat.model.Category;
import com.aikids.care.domain.chat.model.RiskLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatUpdateRequest {
    private Category category; // String 대신 Enum 사용

    @JsonProperty("risk_level")
    private RiskLevel riskLevel; // String 대신 Enum 사용
}