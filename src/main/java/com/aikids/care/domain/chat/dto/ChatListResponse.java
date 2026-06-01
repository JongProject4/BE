package com.aikids.care.domain.chat.dto;

import com.aikids.care.domain.chat.model.Category;
import com.aikids.care.domain.chat.model.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatListResponse {
    private Long chatId;
    private Category category;
    private RiskLevel riskLevel;
    private LocalDateTime createdAt;

    @Builder
    public ChatListResponse(Long chatId, Category category, RiskLevel riskLevel, LocalDateTime createdAt) {
        this.chatId = chatId;
        this.category = category;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
    }
}
