package com.aikids.care.domain.repository;

import com.aikids.care.domain.healthlog.entity.HealthLog;
import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthLogRepository extends JpaRepository<HealthLog, Long> {

    // 아이 ID로 전체 조회 (최신순)
    List<HealthLog> findByChild_IdOrderByEventDateDesc(Long childId);

    // 아이 ID + 로그 타입으로 필터링 조회 (최신순)
    List<HealthLog> findByChild_IdAndLogTypeOrderByEventDateDesc(Long childId, LogType logType);
}