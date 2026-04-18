package com.aikids.care.domain.medicationalarm.controller;

import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmRequest;
import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmResponse;
import com.aikids.care.domain.medicationalarm.service.MedicationAlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/medication-alarms")
@RequiredArgsConstructor
public class MedicationAlarmController {

    private final MedicationAlarmService medicationAlarmService;

    /**
     * GET /api/children/{childId}/medication-alarms
     * 복약 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<MedicationAlarmResponse>> getMedicationAlarms(
            @PathVariable Long childId
    ) {
        return ResponseEntity.ok(medicationAlarmService.getMedicationAlarms(childId));
    }

    /**
     * POST /api/children/{childId}/medication-alarms
     * 복약 알림 등록
     */
    @PostMapping
    public ResponseEntity<MedicationAlarmResponse> createMedicationAlarm(
            @PathVariable Long childId,
            @Valid @RequestBody MedicationAlarmRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(medicationAlarmService.createMedicationAlarm(childId, request));
    }

    /**
     * PUT /api/children/{childId}/medication-alarms/{id}
     * 복약 알림 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<MedicationAlarmResponse> updateMedicationAlarm(
            @PathVariable Long childId,
            @PathVariable Long id,
            @Valid @RequestBody MedicationAlarmRequest request
    ) {
        return ResponseEntity.ok(medicationAlarmService.updateMedicationAlarm(childId, id, request));
    }

    /**
     * DELETE /api/children/{childId}/medication-alarms/{id}
     * 복약 알림 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationAlarm(
            @PathVariable Long childId,
            @PathVariable Long id
    ) {
        medicationAlarmService.deleteMedicationAlarm(childId, id);
        return ResponseEntity.noContent().build();
    }
}