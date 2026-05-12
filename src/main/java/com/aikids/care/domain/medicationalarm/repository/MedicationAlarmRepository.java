package com.aikids.care.domain.medicationalarm.repository;

import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationAlarmRepository extends JpaRepository<MedicationAlarm, Long> {

    List<MedicationAlarm> findByChild_Id(Long childId);
    Optional<MedicationAlarm> findByIdAndChild_Id(Long id, Long childId);
}