package com.aikids.care.domain.hospitalalarm.controller;

import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmRequest;
import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmResponse;
import com.aikids.care.domain.hospitalalarm.service.HospitalAlarmService;
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
@RequestMapping("/api/children/{childId}/hospital-alarms")
@RequiredArgsConstructor
public class HospitalAlarmController {

    private final HospitalAlarmService hospitalAlarmService;

    @GetMapping
    public ResponseEntity<List<HospitalAlarmResponse>> getHospitalAlarms(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity.ok(hospitalAlarmService.getHospitalAlarms(childId, userId));
    }

    @PostMapping
    public ResponseEntity<HospitalAlarmResponse> createHospitalAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @Valid @RequestBody HospitalAlarmRequest request
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hospitalAlarmService.createHospitalAlarm(childId, request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HospitalAlarmResponse> updateHospitalAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @PathVariable Long id,
            @Valid @RequestBody HospitalAlarmRequest request
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity.ok(hospitalAlarmService.updateHospitalAlarm(childId, id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospitalAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @PathVariable Long id
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        hospitalAlarmService.deleteHospitalAlarm(childId, id, userId);
        return ResponseEntity.noContent().build();
    }

}
