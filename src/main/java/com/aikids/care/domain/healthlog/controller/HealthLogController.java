package com.aikids.care.domain.healthlog.controller;

import com.aikids.care.domain.healthlog.dto.HealthLogRequest;
import com.aikids.care.domain.healthlog.dto.HealthLogResponse;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import com.aikids.care.domain.healthlog.service.HealthLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/health-log")
@RequiredArgsConstructor
public class HealthLogController {

    private final HealthLogService healthLogService;

    /**
     * GET /api/children/{childId}/health-log
     * 아이의 헬스로그를 타임라인 형태로 조회
     * logType 쿼리 파라미터로 필터링 가능 (CONSULTATION, MEDICATION, HOSPITAL)
     * CONSULTATION: 상담, MEDICATION: 약 처방, HOSPITAL: 진료
     */
    @GetMapping
    public ResponseEntity<List<HealthLogResponse>> getHealthLogs(
            @PathVariable Long childId,
            @RequestParam(required = false) LogType logType
    ) {
        return ResponseEntity.ok(healthLogService.getHealthLogs(childId, logType));
    }

    /**
     * POST /api/children/{childId}/health-log
     * 헬스로그에 수동으로 기록 추가
     */
    @PostMapping
    public ResponseEntity<HealthLogResponse> createHealthLog(
            @PathVariable Long childId,
            @Valid @RequestBody HealthLogRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(healthLogService.createHealthLog(childId, request));
    }
}