package com.aikids.care.domain.chat.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 부모 테이블 ID는 제거됨 (스키마 반영)
    @Column(name = "child_id")
    private Long childId;

    // 🌟 String에서 Enum으로 변경, NOT NULL 처리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 🌟 String에서 Enum으로 변경, NOT NULL 처리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
    private List<ChatDetail> chatDetails = new ArrayList<>();

    @Builder
    public Chat(Long childId) {
        this.childId = childId;
        // NOT NULL 조건이므로 방 생성 시 '판독 중' 상태로 초기화
        this.category = Category.ANALYZING;
        this.riskLevel = RiskLevel.ANALYZING;
        this.createdAt = LocalDateTime.now();
    }

    // AI 판독 결과 업데이트 메서드 (PATCH API용)
    public void updateResult(Category category, RiskLevel riskLevel) {
        this.category = category;
        this.riskLevel = riskLevel;
    }
}