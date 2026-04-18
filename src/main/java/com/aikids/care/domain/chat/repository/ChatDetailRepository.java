package com.aikids.care.domain.chat.repository;

import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatDetailRepository extends JpaRepository<ChatDetail, Long> {
    // 특정 방(Chat)의 대화 내역을 시간 오름차순(옛날 것부터 최신순)으로 가져오기
    List<ChatDetail> findByChatOrderByCreatedAtAsc(Chat chat);
}