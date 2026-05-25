package com.aikids.care.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiAnalysisResponse {
    @JsonProperty("category") // JSON의 "category" 키를 이 필드에 매핑
    private String category;

    @JsonProperty("riskLevel") // JSON의 "riskLevel" 키를 이 필드에 매핑
    private String riskLevel;
}