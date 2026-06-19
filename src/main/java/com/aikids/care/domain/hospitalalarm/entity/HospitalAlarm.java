package com.aikids.care.domain.hospitalalarm.entity;

import com.aikids.care.domain.child.entity.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_alarm",
        indexes = @Index(name = "idx_hosp_alarm_notify", columnList = "is_active, visit_date, notified_stages"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalAlarm {

    public static final int STAGE_7D = 1;
    public static final int STAGE_3D = 2;
    public static final int STAGE_1D = 4;
    public static final int STAGE_1H = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "hospital_name", nullable = false)
    private String hospitalName;

    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;

    @Column(name = "memo")
    private String memo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 비트마스크: STAGE_7D=1, STAGE_3D=2, STAGE_1D=4, STAGE_1H=8
    @Column(name = "notified_stages", nullable = false)
    private int notifiedStages = 0;

    @Builder
    public HospitalAlarm(Child child, String hospitalName, LocalDateTime visitDate, String memo) {
        this.child = child;
        this.hospitalName = hospitalName;
        this.visitDate = visitDate;
        this.memo = memo;
        this.isActive = true;
        this.notifiedStages = 0;
    }

    public void update(String hospitalName, LocalDateTime visitDate, String memo, Boolean isActive) {
        if (hospitalName != null) this.hospitalName = hospitalName;
        if (visitDate != null) {
            this.visitDate = visitDate;
            this.notifiedStages = 0; // 날짜 변경 시 알림 이력 초기화
        }
        if (memo != null) this.memo = memo;
        if (isActive != null) this.isActive = isActive;
    }

    public void addNotifiedStage(int stageBit) {
        this.notifiedStages |= stageBit;
    }

    /**
     * 등록/수정 시점에 이미 트리거 시각이 지난 단계를 "발송됨"으로 마킹해서
     * 스케줄러가 잘못된 시점에 과거 단계 푸시를 발송하지 않도록 한다.
     *
     * 예) 내일 10시 알람을 오늘 19시에 등록하면 7D/3D 트리거 시각이 이미 지났으므로
     *     해당 단계 마킹 → 1D, 1H만 정상 발송된다.
     */
    public void markPastStagesAsNotified(LocalDateTime now) {
        if (visitDate == null) return;
        if (now.isAfter(visitDate.minusDays(7))) this.notifiedStages |= STAGE_7D;
        if (now.isAfter(visitDate.minusDays(3))) this.notifiedStages |= STAGE_3D;
        if (now.isAfter(visitDate.minusDays(1))) this.notifiedStages |= STAGE_1D;
        if (now.isAfter(visitDate.minusHours(1))) this.notifiedStages |= STAGE_1H;
    }
}
