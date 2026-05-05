package com.aikids.care.domain.medicationalarm.repository;

import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationAlarmRepository extends JpaRepository<MedicationAlarm, Long> {

    // 아이 ID로 복약 알림 목록 조회
    List<MedicationAlarm> findByChild_Id(Long childId);
}