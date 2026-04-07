package com.aikids.care.domain.healthlog.service;

import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.healthlog.dto.HealthLogRequest;
import com.aikids.care.domain.healthlog.dto.HealthLogResponse;
import com.aikids.care.domain.healthlog.entity.HealthLog;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import com.aikids.care.domain.healthlog.repository.HealthLogRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthLogService {

    private final HealthLogRepository healthLogRepository;
    private final ChildRepository childRepository;

    // 헬스 로그 타임라인 조회
    public List<HealthLogResponse> getHealthLogs(Long childId, LogType logType) {
        validateChild(childId);

        List<HealthLog> logs;
        if (logType == null) {
            logs = healthLogRepository.findByChild_IdOrderByEventDateDesc(childId);
        } else {
            logs = healthLogRepository.findByChild_IdAndLogTypeOrderByEventDateDesc(childId, logType);
        }

        return logs.stream()
                .map(HealthLogResponse::from)
                .collect(Collectors.toList());
    }

    // 헬스 로그 수동 추가
    @Transactional
    public HealthLogResponse createHealthLog(Long childId, HealthLogRequest request) {
        Child child = validateChild(childId);

        HealthLog healthLog = HealthLog.builder()
                .child(child)
                .logType(request.getLogType())
                .content(request.getContent())
                .eventDate(request.getEventDate())
                .build();

        return HealthLogResponse.from(healthLogRepository.save(healthLog));
    }

    private Child validateChild(Long childId) {
        return childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
    }
}