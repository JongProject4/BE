package com.aikids.care.domain.healthlog.controller;

import com.aikids.care.domain.healthlog.dto.HealthLogRequest;
import com.aikids.care.domain.healthlog.dto.HealthLogResponse;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import com.aikids.care.domain.healthlog.service.HealthLogService;
import com.aikids.care.global.security.OAuth2Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/health-log")
@RequiredArgsConstructor
public class HealthLogController {

    private final HealthLogService healthLogService;

    @GetMapping
    public ResponseEntity<List<HealthLogResponse>> getHealthLogs(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @RequestParam(required = false) LogType logType
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity.ok(healthLogService.getHealthLogs(childId, logType, userId));
    }

    @PostMapping
    public ResponseEntity<HealthLogResponse> createHealthLog(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @Valid @RequestBody HealthLogRequest request
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(healthLogService.createHealthLog(childId, request, userId));
    }

}