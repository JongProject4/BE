package com.aikids.care.domain.healthlog.dto;

import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class HealthLogRequest {

    @NotNull(message = "로그 유형은 필수입니다.")
    private LogType logType;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "사건 발생 일시는 필수입니다.")
    private LocalDateTime eventDate;
}