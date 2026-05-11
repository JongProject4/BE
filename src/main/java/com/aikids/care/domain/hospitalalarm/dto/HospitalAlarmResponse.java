package com.aikids.care.domain.hospitalalarm.dto;

import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HospitalAlarmResponse {

    private Long id;
    private Long childId;
    private String hospitalName;
    private LocalDateTime visitDate;
    private String memo;
    private Boolean isActive;

    public static HospitalAlarmResponse from(HospitalAlarm hospitalAlarm) {
        return HospitalAlarmResponse.builder()
                .id(hospitalAlarm.getId())
                .childId(hospitalAlarm.getChild().getId())
                .hospitalName(hospitalAlarm.getHospitalName())
                .visitDate(hospitalAlarm.getVisitDate())
                .memo(hospitalAlarm.getMemo())
                .isActive(hospitalAlarm.getIsActive())
                .build();
    }
}
