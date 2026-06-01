package com.aikids.care.domain.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device",
        uniqueConstraints = @UniqueConstraint(columnNames = "fcm_token"),
        indexes = @Index(name = "idx_user_device_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Builder
    public UserDevice(User user, String fcmToken) {
        this.user = user;
        this.fcmToken = fcmToken;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void refreshLastSeen(User user) {
        this.user = user;
        this.lastSeenAt = LocalDateTime.now();
    }
}
