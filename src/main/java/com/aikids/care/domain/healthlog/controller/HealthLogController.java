package com.aikids.care.domain.healthlog.controller;

import com.aikids.care.domain.healthlog.dto.HealthLogRequest;
import com.aikids.care.domain.healthlog.dto.HealthLogResponse;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import com.aikids.care.domain.healthlog.service.HealthLogService;
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
        Long userId = extractUserId(oauth2User);
        return ResponseEntity.ok(healthLogService.getHealthLogs(childId, logType, userId));
    }

    @PostMapping
    public ResponseEntity<HealthLogResponse> createHealthLog(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @Valid @RequestBody HealthLogRequest request
    ) {
        Long userId = extractUserId(oauth2User);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(healthLogService.createHealthLog(childId, request, userId));
    }

    private Long extractUserId(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("Unauthenticated user.");
        }
        Object userId = oauth2User.getAttributes().get("userId");
        if (userId == null) {
            throw new IllegalStateException("OAuth2 attributes are missing userId.");
        }
        return ((Number) userId).longValue();
    }
}