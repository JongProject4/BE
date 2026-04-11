package com.aikids.care.domain.chat.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;     // 사용자 식별자
    private Long childId;
    private String question;   // 내가 보낸 질문

    @Column(columnDefinition = "TEXT")
    private String answer;     // AI의 답변

    private LocalDateTime createdAt; // 저장 시간

    @Builder
    public Chat(Long userId, Long childId, String question, String answer) {
        this.userId = userId;
        this.childId = childId;
        this.question = question;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }
}