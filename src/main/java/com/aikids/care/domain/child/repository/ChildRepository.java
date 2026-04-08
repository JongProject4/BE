package com.aikids.care.domain.child.repository;

import com.aikids.care.domain.child.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, Long> {

    // 특정 유저의 아이 목록 조회
    List<Child> findByUser_Id(Long userId);

    // 특정 유저의 특정 아이 조회
    Optional<Child> findByIdAndUser_Id(Long childId, Long userId);
}