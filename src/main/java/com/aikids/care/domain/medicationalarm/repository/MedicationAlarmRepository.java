package com.aikids.care.domain.medicationalarm.repository;

import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicationAlarmRepository extends JpaRepository<MedicationAlarm, Long> {

    List<MedicationAlarm> findByChild_IdAndIsActiveTrue(Long childId);

    Optional<MedicationAlarm> findByIdAndChild_Id(Long id, Long childId);

    @Query("SELECT m FROM MedicationAlarm m JOIN FETCH m.child c JOIN FETCH c.user " +
           "WHERE m.isActive = true AND m.nextNotifyAt IS NOT NULL AND m.nextNotifyAt <= :now")
    List<MedicationAlarm> findAlarmsToNotify(@Param("now") LocalDateTime now);
}
