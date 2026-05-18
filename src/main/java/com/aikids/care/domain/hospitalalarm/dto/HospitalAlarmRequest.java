package com.aikids.care.domain.hospitalalarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class HospitalAlarmRequest {

    @NotBlank(message = "병원 이름은 필수입니다.")
    private String hospitalName;

    @NotNull(message = "방문 일시는 필수입니다.")
    private LocalDateTime visitDate;

    private String memo;
}
