package com.aikids.care.domain.chat.alarm;

import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingAlarmDraftStore {

    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AlarmDraft load(Long chatId) {
        return chatRepository.findById(chatId)
                .map(Chat::getPendingAlarmDraft)
                .map(this::deserialize)
                .orElse(AlarmDraft.none());
    }

    @Transactional
    public void save(Long chatId, AlarmDraft draft) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        chat.updatePendingAlarmDraft(serialize(draft));
    }

    @Transactional
    public void clear(Long chatId) {
        chatRepository.findById(chatId)
                .ifPresent(chat -> chat.updatePendingAlarmDraft(null));
    }

    private String serialize(AlarmDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            log.error("[AlarmDraftStore] serialize failed", e);
            throw new IllegalStateException("AlarmDraft serialize failed", e);
        }
    }

    private AlarmDraft deserialize(String json) {
        if (json == null || json.isBlank()) return AlarmDraft.none();
        try {
            AlarmDraft parsed = objectMapper.readValue(json, AlarmDraft.class);
            return parsed == null ? AlarmDraft.none() : parsed;
        } catch (Exception e) {
            log.warn("[AlarmDraftStore] deserialize failed json='{}'", json, e);
            return AlarmDraft.none();
        }
    }
}
