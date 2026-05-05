package com.aikids.care.domain.medicationalarm.service;

import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmRequest;
import com.aikids.care.domain.medicationalarm.dto.MedicationAlarmResponse;
import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import com.aikids.care.domain.medicationalarm.repository.MedicationAlarmRepository;
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
public class MedicationAlarmService {

    private final MedicationAlarmRepository medicationAlarmRepository;
    private final ChildRepository childRepository;

    // 복약 알림 목록 조회
    public List<MedicationAlarmResponse> getMedicationAlarms(Long childId) {
        validateChild(childId);
        return medicationAlarmRepository.findByChild_Id(childId)
                .stream()
                .map(MedicationAlarmResponse::from)
                .collect(Collectors.toList());
    }

    // 복약 알림 등록
    @Transactional
    public MedicationAlarmResponse createMedicationAlarm(Long childId, MedicationAlarmRequest request) {
        Child child = validateChild(childId);

        MedicationAlarm medicationAlarm = MedicationAlarm.builder()
                .child(child)
                .medicineName(request.getMedicineName())
                .dosage(request.getDosage())
                .intervalHour(request.getIntervalHour())
                .build();

        return MedicationAlarmResponse.from(medicationAlarmRepository.save(medicationAlarm));
    }

    // 복약 알림 수정
    @Transactional
    public MedicationAlarmResponse updateMedicationAlarm(Long childId, Long alarmId, MedicationAlarmRequest request) {
        validateChild(childId);

        MedicationAlarm medicationAlarm = medicationAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_ALARM_NOT_FOUND));

        medicationAlarm.update(request.getMedicineName(), request.getDosage(),
                request.getIntervalHour(), null);

        return MedicationAlarmResponse.from(medicationAlarm);
    }

    // 복약 알림 삭제
    @Transactional
    public void deleteMedicationAlarm(Long childId, Long alarmId) {
        validateChild(childId);

        MedicationAlarm medicationAlarm = medicationAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_ALARM_NOT_FOUND));

        medicationAlarmRepository.delete(medicationAlarm);
    }

    private Child validateChild(Long childId) {
        return childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
    }
}