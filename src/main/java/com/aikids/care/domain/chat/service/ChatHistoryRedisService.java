package com.aikids.care.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatHistoryRedisService {

    private static final String KEY_PREFIX = "chat:history:";
    private static final long TTL_HOURS = 24;
    private static final int MAX_TURNS = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void addMessage(Long chatId, String role, String content) {
        String key = key(chatId);
        List<Map<String, String>> history = getHistory(chatId);
        history.add(Map.of("role", role, "content", content));

        // 최근 MAX_TURNS * 2개 메시지만 유지
        if (history.size() > MAX_TURNS * 2) {
            history = history.subList(history.size() - MAX_TURNS * 2, history.size());
        }

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(history), TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 히스토리 직렬화 실패", e);
        }
    }

    public List<Map<String, String>> getHistory(Long chatId) {
        String json = redisTemplate.opsForValue().get(key(chatId));
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void deleteHistory(Long chatId) {
        redisTemplate.delete(key(chatId));
    }

    private String key(Long chatId) {
        return KEY_PREFIX + chatId;
    }
}
