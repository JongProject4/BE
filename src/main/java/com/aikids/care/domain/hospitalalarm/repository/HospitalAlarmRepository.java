package com.aikids.care.domain.hospitalalarm.repository;

import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalAlarmRepository extends JpaRepository<HospitalAlarm, Long> {
    List<HospitalAlarm> findByChild_Id(Long childId);
}
