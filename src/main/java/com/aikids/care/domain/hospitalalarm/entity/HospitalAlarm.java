package com.aikids.care.domain.hospitalalarm.entity;

import com.aikids.care.domain.child.entity.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_alarm")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalAlarm {

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

    @Builder
    public HospitalAlarm(Child child, String hospitalName, LocalDateTime visitDate, String memo) {
        this.child = child;
        this.hospitalName = hospitalName;
        this.visitDate = visitDate;
        this.memo = memo;
        this.isActive = true;
    }

    public void update(String hospitalName, LocalDateTime visitDate, String memo, Boolean isActive) {
        if (hospitalName != null) this.hospitalName = hospitalName;
        if (visitDate != null) this.visitDate = visitDate;
        if (memo != null) this.memo = memo;
        if (isActive != null) this.isActive = isActive;
    }
}
