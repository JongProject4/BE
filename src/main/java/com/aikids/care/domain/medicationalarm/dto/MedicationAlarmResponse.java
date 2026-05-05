package com.aikids.care.domain.medicationalarm.dto;

import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicationAlarmResponse {

    private Long id;
    private Long childId;
    private String medicineName;
    private String dosage;
    private Integer intervalHour;
    private Boolean isActive;

    public static MedicationAlarmResponse from(MedicationAlarm medicationAlarm) {
        return MedicationAlarmResponse.builder()
                .id(medicationAlarm.getId())
                .childId(medicationAlarm.getChild().getId())
                .medicineName(medicationAlarm.getMedicineName())
                .dosage(medicationAlarm.getDosage())
                .intervalHour(medicationAlarm.getIntervalHour())
                .isActive(medicationAlarm.getIsActive())
                .build();
    }
}