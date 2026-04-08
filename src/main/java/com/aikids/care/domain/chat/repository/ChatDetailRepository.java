package com.aikids.care.domain.chat.repository;

import com.aikids.care.domain.chat.model.ChatDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatDetailRepository extends JpaRepository<ChatDetail, Long> {
}