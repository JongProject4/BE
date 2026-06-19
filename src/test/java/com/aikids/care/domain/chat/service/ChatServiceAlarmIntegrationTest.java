package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.alarm.AlarmFlowHandler;
import com.aikids.care.domain.chat.dto.ChatMessageRequest;
import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import com.aikids.care.infra.gemini.GeminiApiClient;
import com.aikids.care.infra.stt.GoogleSttClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceAlarmIntegrationTest {

    private static final Long CHAT_ID = 100L;
    private static final Long USER_ID = 10L;
    private static final Long CHILD_ID = 7L;
    private static final String SOCIAL_ID = "social-1";

    private ChatRepository chatRepository;
    private ChatDetailRepository chatDetailRepository;
    private ChatMessagePersistence chatMessagePersistence;
    private GoogleSttClient googleSttClient;
    private VoiceChatStreamPipeline voiceChatStreamPipeline;
    private GeminiApiClient geminiApiClient;
    private ChatAiService chatAiService;
    private TransactionTemplate transactionTemplate;
    private UserRepository userRepository;
    private ChildRepository childRepository;
    private AlarmFlowHandler alarmFlowHandler;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatRepository = mock(ChatRepository.class);
        chatDetailRepository = mock(ChatDetailRepository.class);
        chatMessagePersistence = mock(ChatMessagePersistence.class);
        googleSttClient = mock(GoogleSttClient.class);
        voiceChatStreamPipeline = mock(VoiceChatStreamPipeline.class);
        geminiApiClient = mock(GeminiApiClient.class);
        chatAiService = mock(ChatAiService.class);
        transactionTemplate = mock(TransactionTemplate.class);
        userRepository = mock(UserRepository.class);
        childRepository = mock(ChildRepository.class);
        alarmFlowHandler = mock(AlarmFlowHandler.class);

        chatService = new ChatService(
                chatRepository, chatDetailRepository, chatMessagePersistence,
                googleSttClient, voiceChatStreamPipeline, geminiApiClient,
                chatAiService, transactionTemplate, userRepository, childRepository,
                alarmFlowHandler
        );

        // ownership validation passes by default
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.findBySocialIdAndSocialType(SOCIAL_ID, SocialType.KAKAO))
                .thenReturn(Optional.of(user));

        Chat chat = Chat.builder().childId(CHILD_ID).build();
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        Child child = mock(Child.class);
        when(childRepository.findByIdAndUser_Id(CHILD_ID, USER_ID)).thenReturn(Optional.of(child));

        // transactionTemplate executes callback inline
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("sendMessage: 알람 흐름이 응답을 반환하면 LLM 호출 없이 메시지 저장 후 응답 그대로 반환")
    void alarmReplyShortCircuitsLlm() {
        when(alarmFlowHandler.handle(eq(CHAT_ID), eq(USER_ID), eq(CHILD_ID), anyString()))
                .thenReturn(Optional.of("타이레놀 5ml 알람을 24시간마다 알려드릴게요."));

        ChatMessageRequest request = new ChatMessageRequest();
        setField(request, "content", "타이레놀 5ml 24시간마다 알림 등록해줘");

        String result = chatService.sendMessage(CHAT_ID, SOCIAL_ID, SocialType.KAKAO, request);

        assertThat(result).isEqualTo("타이레놀 5ml 알람을 24시간마다 알려드릴게요.");
        verify(geminiApiClient, never()).askText(anyString(), anyList(), any(), any());
        verify(chatDetailRepository, org.mockito.Mockito.times(2))
                .save(any(com.aikids.care.domain.chat.model.ChatDetail.class));
    }

    @Test
    @DisplayName("sendMessage: 알람 흐름이 빈 응답이면 일반 상담 LLM 호출")
    void noAlarmFallsThroughToLlm() {
        when(alarmFlowHandler.handle(eq(CHAT_ID), eq(USER_ID), eq(CHILD_ID), anyString()))
                .thenReturn(Optional.empty());
        when(chatDetailRepository.findByChatIdOrderByCreatedAtAsc(CHAT_ID))
                .thenReturn(java.util.List.of());
        when(geminiApiClient.askText(anyString(), anyList(), any(), any()))
                .thenReturn("일반 상담 답변");

        ChatMessageRequest request = new ChatMessageRequest();
        setField(request, "content", "아이가 열이 38도예요");

        String result = chatService.sendMessage(CHAT_ID, SOCIAL_ID, SocialType.KAKAO, request);

        assertThat(result).isEqualTo("일반 상담 답변");
        verify(geminiApiClient).askText(anyString(), anyList(), any(), any());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
