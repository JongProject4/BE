package com.aikids.care.domain.chat.model;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

@Entity // "이 클래스는 데이터베이스 테이블과 연결된 원본 설계도입니다!"
@Getter // 롬복(Lombok) 도구: 데이터 값을 쉽게 꺼내볼 수 있게 해줍니다.
public class Chat {

    @Id // 이 테이블의 대표키(PK)임을 표시
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 번호를 1, 2, 3... 자동으로 매겨줌
    private Long id;

    // 다른 부서(Child)의 정보이므로 일단 아이디(번호)만 저장해 둡니다.
    @Column(name = "child_id")
    private Long childId;

    @Enumerated(EnumType.STRING) // Enum(카테고리) 값을 글자 그대로 DB에 저장하라는 뜻
    private Category category;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private LocalDateTime createdAt = LocalDateTime.now(); // 생성될 때 현재 시간 자동 기록
}