package com.aikids.care.domain.medicatonalarm.entity;

import com.aikids.care.domain.child.entity.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medication_alarm")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "medicine_name")
    private String medicineName;

    private String dosage;

    @Column(name = "interval_hour")
    private Integer intervalHour;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public MedicationAlarm(Child child, String medicineName, String dosage, Integer intervalHour) {
        this.child = child;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.intervalHour = intervalHour;
        this.isActive = true;
    }

    public void update(String medicineName, String dosage, Integer intervalHour, Boolean isActive) {
        if (medicineName != null) this.medicineName = medicineName;
        if (dosage != null) this.dosage = dosage;
        if (intervalHour != null) this.intervalHour = intervalHour;
        if (isActive != null) this.isActive = isActive;
    }
}