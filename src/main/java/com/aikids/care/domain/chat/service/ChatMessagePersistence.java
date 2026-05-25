package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessagePersistence {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;

    @Transactional
    public void saveUserTranscript(Long chatId, String content) {
        Chat chat = findChat(chatId);
        chatDetailRepository.save(ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(content)
                .build());
    }

    @Transactional
    public void saveAiReply(Long chatId, String content) {
        Chat chat = findChat(chatId);
        chatDetailRepository.save(ChatDetail.builder()
                .chat(chat)
                .role(Role.AI)
                .content(content)
                .build());
    }

    private Chat findChat(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));
    }
}
