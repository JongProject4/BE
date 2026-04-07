package com.aikids.care.domain.healthlog.dto;

import com.aikids.care.domain.healthlog.entity.HealthLog;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HealthLogResponse {

    private Long id;
    private Long childId;
    private LogType logType;
    private String content;
    private LocalDateTime eventDate;

    public static HealthLogResponse from(HealthLog healthLog) {
        return HealthLogResponse.builder()
                .id(healthLog.getId())
                .childId(healthLog.getChild().getId())
                .logType(healthLog.getLogType())
                .content(healthLog.getContent())
                .eventDate(healthLog.getEventDate())
                .build();
    }
}