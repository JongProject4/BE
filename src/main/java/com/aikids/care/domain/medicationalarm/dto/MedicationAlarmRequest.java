package com.aikids.care.domain.medicationalarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MedicationAlarmRequest {

    @NotBlank(message = "약 이름은 필수입니다.")
    private String medicineName;

    @NotBlank(message = "복용량은 필수입니다.")
    private String dosage;

    @NotNull(message = "복용 간격은 필수입니다.")
    private Integer intervalHour;
}