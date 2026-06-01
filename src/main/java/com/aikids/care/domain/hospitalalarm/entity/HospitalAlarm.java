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
}
