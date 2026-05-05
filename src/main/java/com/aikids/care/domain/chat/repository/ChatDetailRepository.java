package com.aikids.care.domain.chat.repository;

import com.aikids.care.domain.chat.model.ChatDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatDetailRepository extends JpaRepository<ChatDetail, Long> {
    List<ChatDetail> findByChatIdOrderByCreatedAtAsc(Long chatId);
}