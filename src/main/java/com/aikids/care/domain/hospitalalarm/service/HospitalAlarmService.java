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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalAlarmService {

    private final HospitalAlarmRepository hospitalAlarmRepository;
    private final ChildRepository childRepository;

    // 내원 알림 목록 조회
    public List<HospitalAlarmResponse> getHospitalAlarms(Long childId, Long userId) {
        validateChild(childId, userId);
        return hospitalAlarmRepository.findByChild_IdAndIsActiveTrue(childId)
                .stream()
                .map(HospitalAlarmResponse::from)
                .toList();
    }

    // 내원 알림 등록
    @Transactional
    public HospitalAlarmResponse createHospitalAlarm(Long childId, HospitalAlarmRequest request, Long userId) {
        Child child = validateChild(childId, userId);

        HospitalAlarm hospitalAlarm = HospitalAlarm.builder()
                .child(child)
                .hospitalName(request.getHospitalName())
                .visitDate(request.getVisitDate())
                .memo(request.getMemo())
                .build();
        hospitalAlarm.markPastStagesAsNotified(LocalDateTime.now());

        return HospitalAlarmResponse.from(hospitalAlarmRepository.save(hospitalAlarm));
    }

    // AI 채팅에서 추출된 필드로 직접 등록 (DTO 우회용 내부 진입점)
    @Transactional
    public HospitalAlarmResponse register(Long childId, Long userId, String hospitalName, LocalDateTime visitDate, String memo) {
        Child child = validateChild(childId, userId);

        HospitalAlarm hospitalAlarm = HospitalAlarm.builder()
                .child(child)
                .hospitalName(hospitalName)
                .visitDate(visitDate)
                .memo(memo)
                .build();
        hospitalAlarm.markPastStagesAsNotified(LocalDateTime.now());

        return HospitalAlarmResponse.from(hospitalAlarmRepository.save(hospitalAlarm));
    }

    // 내원 알림 수정
    @Transactional
    public HospitalAlarmResponse updateHospitalAlarm(Long childId, Long alarmId, HospitalAlarmRequest request, Long userId) {
        validateChild(childId, userId);

        HospitalAlarm hospitalAlarm = hospitalAlarmRepository.findByIdAndChild_Id(alarmId, childId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_ALARM_NOT_FOUND));

        hospitalAlarm.update(request.getHospitalName(), request.getVisitDate(),
                request.getMemo(), request.getIsActive());
        // 엔티티 update에서 visitDate 변경 시 notifiedStages를 0으로 리셋함.
        // 그 위에 지난 단계를 다시 마킹해야 새 visitDate 기준 정확한 발송이 가능.
        if (request.getVisitDate() != null) {
            hospitalAlarm.markPastStagesAsNotified(LocalDateTime.now());
        }

        return HospitalAlarmResponse.from(hospitalAlarm);
    }

    // 내원 알림 삭제
    @Transactional
    public void deleteHospitalAlarm(Long childId, Long alarmId, Long userId) {
        validateChild(childId, userId);

        HospitalAlarm hospitalAlarm = hospitalAlarmRepository.findByIdAndChild_Id(alarmId, childId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_ALARM_NOT_FOUND));

        hospitalAlarmRepository.delete(hospitalAlarm);
    }

    private Child validateChild(Long childId, Long userId) {
        return childRepository.findByIdAndUser_Id(childId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
    }
}
