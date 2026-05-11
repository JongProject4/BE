package com.aikids.care.domain.hospitalalarm.controller;

import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmRequest;
import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmResponse;
import com.aikids.care.domain.hospitalalarm.service.HospitalAlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/hospital-alarms")
@RequiredArgsConstructor
public class HospitalAlarmController {

    private final HospitalAlarmService hospitalAlarmService;

    /**
     * GET /api/children/{childId}/hospital-alarms
     * 내원 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<HospitalAlarmResponse>> getHospitalAlarms(
            @PathVariable Long childId
    ) {
        return ResponseEntity.ok(hospitalAlarmService.getHospitalAlarms(childId));
    }

    /**
     * POST /api/children/{childId}/hospital-alarms
     * 내원 알림 등록
     */
    @PostMapping
    public ResponseEntity<HospitalAlarmResponse> createHospitalAlarm(
            @PathVariable Long childId,
            @Valid @RequestBody HospitalAlarmRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hospitalAlarmService.createHospitalAlarm(childId, request));
    }

    /**
     * PUT /api/children/{childId}/hospital-alarms/{id}
     * 내원 알림 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<HospitalAlarmResponse> updateHospitalAlarm(
            @PathVariable Long childId,
            @PathVariable Long id,
            @Valid @RequestBody HospitalAlarmRequest request
    ) {
        return ResponseEntity.ok(hospitalAlarmService.updateHospitalAlarm(childId, id, request));
    }

    /**
     * DELETE /api/children/{childId}/hospital-alarms/{id}
     * 내원 알림 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospitalAlarm(
            @PathVariable Long childId,
            @PathVariable Long id
    ) {
        hospitalAlarmService.deleteHospitalAlarm(childId, id);
        return ResponseEntity.noContent().build();
    }
}
