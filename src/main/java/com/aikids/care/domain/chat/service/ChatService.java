package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.model.ChatDetail;
import com.aikids.care.domain.chat.model.Role;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.infra.gemini.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // "이 클래스는 주방장(핵심 기능)입니다!" 라는 명찰
@RequiredArgsConstructor // 창고 관리인, 통신병 등 필요한 직원을 알아서 불러옵니다.
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final GeminiApiClient geminiApiClient;

    // 표의 2번째 줄 API: 특정 상담 세션에 메시지를 보내고 AI 답변을 받는 기능
    @Transactional // "이 과정 중 하나라도 에러가 나면 싹 다 취소해!" (데이터 보호용 안전장치)
    public String sendMessage(Long chatId, ChatMessageRequest request) {

        // 1. 냉장고에서 번호표(chatId)에 맞는 바인더(Chat) 꺼내오기
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담 세션을 찾을 수 없습니다."));

        // 2. 부모님이 보낸 메시지를 낱장 종이로 만들어서 냉장고에 저장하기 (Role: USER)
        ChatDetail userMessage = ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(request.getContent())
                .build();
        chatDetailRepository.save(userMessage);

        // 3. 외부 통신병에게 부모님 메시지 넘겨주고 제미나이의 답변 받아오기
        String aiResponseText = geminiApiClient.askToGemini(request.getContent());

        // 4. 제미나이가 준 답변도 낱장 종이로 만들어서 냉장고에 저장하기 (Role: AI)
        ChatDetail aiMessage = ChatDetail.builder()
                .chat(chat)
                .role(Role.AI)
                .content(aiResponseText)
                .build();
        chatDetailRepository.save(aiMessage);

        // 5. 최종적으로 완성된 제미나이의 답변 텍스트를 반환
        return aiResponseText;
    }
}