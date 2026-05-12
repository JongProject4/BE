package com.aikids.care.domain.hospitalalarm.repository;

import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalAlarmRepository extends JpaRepository<HospitalAlarm, Long> {
    List<HospitalAlarm> findByChild_Id(Long childId);
    Optional<HospitalAlarm> findByIdAndChild_Id(Long id, Long childId);
}
