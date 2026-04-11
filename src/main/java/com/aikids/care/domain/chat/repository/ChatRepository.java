package com.aikids.care.domain.chat.repository;

import com.aikids.care.domain.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    // 특정 아이(childId)의 대화 내역을 최신순으로 가져오기
    List<Chat> findByChildIdOrderByCreatedAtDesc(Long childId);
}