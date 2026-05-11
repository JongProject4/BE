package com.aikids.care.domain.hospitalalarm.service;

import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmRequest;
import com.aikids.care.domain.hospitalalarm.dto.HospitalAlarmResponse;
import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import com.aikids.care.domain.hospitalalarm.repository.HospitalAlarmRepository;
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
public class HospitalAlarmService {

    private final HospitalAlarmRepository hospitalAlarmRepository;
    private final ChildRepository childRepository;

    // 내원 알림 목록 조회
    public List<HospitalAlarmResponse> getHospitalAlarms(Long childId) {
        validateChild(childId);
        return hospitalAlarmRepository.findByChild_Id(childId)
                .stream()
                .map(HospitalAlarmResponse::from)
                .collect(Collectors.toList());
    }

    // 내원 알림 등록
    @Transactional
    public HospitalAlarmResponse createHospitalAlarm(Long childId, HospitalAlarmRequest request) {
        Child child = validateChild(childId);

        HospitalAlarm hospitalAlarm = HospitalAlarm.builder()
                .child(child)
                .hospitalName(request.getHospitalName())
                .visitDate(request.getVisitDate())
                .memo(request.getMemo())
                .build();

        return HospitalAlarmResponse.from(hospitalAlarmRepository.save(hospitalAlarm));
    }

    // 내원 알림 수정
    @Transactional
    public HospitalAlarmResponse updateHospitalAlarm(Long childId, Long alarmId, HospitalAlarmRequest request) {
        validateChild(childId);

        HospitalAlarm hospitalAlarm = hospitalAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_ALARM_NOT_FOUND));

        hospitalAlarm.update(request.getHospitalName(), request.getVisitDate(),
                request.getMemo(), null);

        return HospitalAlarmResponse.from(hospitalAlarm);
    }

    // 내원 알림 삭제
    @Transactional
    public void deleteHospitalAlarm(Long childId, Long alarmId) {
        validateChild(childId);

        HospitalAlarm hospitalAlarm = hospitalAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_ALARM_NOT_FOUND));

        hospitalAlarmRepository.delete(hospitalAlarm);
    }

    private Child validateChild(Long childId) {
        return childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
    }
}
