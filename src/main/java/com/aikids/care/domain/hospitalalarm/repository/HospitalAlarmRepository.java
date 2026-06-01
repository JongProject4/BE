package com.aikids.care.domain.hospitalalarm.repository;

import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HospitalAlarmRepository extends JpaRepository<HospitalAlarm, Long> {

    List<HospitalAlarm> findByChild_IdAndIsActiveTrue(Long childId);

    Optional<HospitalAlarm> findByIdAndChild_Id(Long id, Long childId);

    // 비트마스크 필터는 네이티브 쿼리로, 엔티티 로딩은 별도 JPQL로 분리
    @Query(value = "SELECT h.id FROM hospital_alarm h " +
                   "WHERE h.is_active = true AND (h.notified_stages & :stageBit) = 0 " +
                   "AND h.visit_date > :now AND h.visit_date <= :threshold",
           nativeQuery = true)
    List<Long> findAlarmIdsToNotify(
            @Param("stageBit") int stageBit,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);

    @Query("SELECT h FROM HospitalAlarm h JOIN FETCH h.child c JOIN FETCH c.user WHERE h.id IN :ids")
    List<HospitalAlarm> findByIdsWithUser(@Param("ids") List<Long> ids);
}
