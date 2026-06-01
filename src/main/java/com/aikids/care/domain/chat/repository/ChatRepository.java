package com.aikids.care.domain.chat.repository;

import com.aikids.care.domain.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByChildIdOrderByCreatedAtDesc(Long childId);

    List<Chat> findByChildIdAndCreatedAtBetween(Long childId, LocalDateTime start, LocalDateTime end);
}