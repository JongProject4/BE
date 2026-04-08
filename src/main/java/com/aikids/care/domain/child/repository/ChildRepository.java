package com.aikids.care.domain.child.repository;

import com.aikids.care.domain.child.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {

    // 특정 유저의 아이 목록 조회
    List<Child> findByUser_Id(Long userId);
}