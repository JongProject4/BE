package com.aikids.care.domain.medicationalarm.controller;

import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmRequest;
import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmResponse;
import com.aikids.care.domain.medicationalarm.service.MedicationAlarmService;
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
@RequestMapping("/api/children/{childId}/medication-alarms")
@RequiredArgsConstructor
public class MedicationAlarmController {

    private final MedicationAlarmService medicationAlarmService;

    @GetMapping
    public ResponseEntity<List<MedicationAlarmResponse>> getMedicationAlarms(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity.ok(medicationAlarmService.getMedicationAlarms(childId, userId));
    }

    @PostMapping
    public ResponseEntity<MedicationAlarmResponse> createMedicationAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @Valid @RequestBody MedicationAlarmRequest request
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(medicationAlarmService.createMedicationAlarm(childId, request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationAlarmResponse> updateMedicationAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @PathVariable Long id,
            @Valid @RequestBody MedicationAlarmRequest request
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        return ResponseEntity.ok(medicationAlarmService.updateMedicationAlarm(childId, id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationAlarm(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long childId,
            @PathVariable Long id
    ) {
        Long userId = OAuth2Utils.extractUserId(oauth2User);
        medicationAlarmService.deleteMedicationAlarm(childId, id, userId);
        return ResponseEntity.noContent().build();
    }

}