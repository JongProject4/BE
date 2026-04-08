package com.aikids.care.domain.healthlog.entity;

import com.aikids.care.domain.child.entity.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType logType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Builder
    public HealthLog(Child child, LogType logType, String content, LocalDateTime eventDate) {
        this.child = child;
        this.logType = logType;
        this.content = content;
        this.eventDate = eventDate;
    }

    public enum LogType {
        CONSULTATION,   // 상담
        MEDICATION,     // 복약
        HOSPITAL        // 내원
    }
}