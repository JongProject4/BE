package com.aikids.care.domain.chat.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder // 새로 추가! (데이터를 쉽게 쏙쏙 넣을 수 있게 해줌)
@NoArgsConstructor // 새로 추가! (기본 뼈대 생성)
@AllArgsConstructor // 새로 추가! (모든 재료를 넣을 수 있게 해줌)
public class ChatDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String audioUrl;

    private String imageUrl;
}