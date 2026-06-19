package com.aikids.care.domain.chat.alarm;

import com.aikids.care.domain.chat.model.Chat;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.global.error.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingAlarmDraftStoreTest {

    private ChatRepository chatRepository;
    private PendingAlarmDraftStore store;

    @BeforeEach
    void setUp() {
        chatRepository = mock(ChatRepository.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        store = new PendingAlarmDraftStore(chatRepository, mapper);
    }

    @Test
    @DisplayName("save → load 라운드트립: 복약 슬롯 보존")
    void roundTripMedication() {
        Chat chat = Chat.builder().childId(7L).build();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        AlarmDraft draft = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .dosage("5ml")
                .intervalHour(24)
                .build();

        store.save(1L, draft);
        AlarmDraft loaded = store.load(1L);

        assertThat(loaded.getIntent()).isEqualTo(AlarmIntent.MEDICATION);
        assertThat(loaded.getMedicineName()).isEqualTo("타이레놀");
        assertThat(loaded.getDosage()).isEqualTo("5ml");
        assertThat(loaded.getIntervalHour()).isEqualTo(24);
    }

    @Test
    @DisplayName("save → load 라운드트립: LocalDateTime 보존 (Jackson JSR310)")
    void roundTripHospitalVisitDate() {
        Chat chat = Chat.builder().childId(7L).build();
        when(chatRepository.findById(2L)).thenReturn(Optional.of(chat));

        AlarmDraft draft = AlarmDraft.builder()
                .intent(AlarmIntent.HOSPITAL)
                .hospitalName("서울아이병원")
                .visitDate(LocalDateTime.of(2026, 6, 25, 14, 0))
                .memo("정기검진")
                .build();

        store.save(2L, draft);
        AlarmDraft loaded = store.load(2L);

        assertThat(loaded.getVisitDate()).isEqualTo(LocalDateTime.of(2026, 6, 25, 14, 0));
        assertThat(loaded.getHospitalName()).isEqualTo("서울아이병원");
        assertThat(loaded.getMemo()).isEqualTo("정기검진");
    }

    @Test
    @DisplayName("clear 후 load → NONE")
    void clearWipesDraft() {
        Chat chat = Chat.builder().childId(7L).build();
        chat.updatePendingAlarmDraft("{\"intent\":\"MEDICATION\"}");
        when(chatRepository.findById(3L)).thenReturn(Optional.of(chat));

        store.clear(3L);
        AlarmDraft loaded = store.load(3L);

        assertThat(loaded.getIntent()).isEqualTo(AlarmIntent.NONE);
    }

    @Test
    @DisplayName("존재하지 않는 chatId load → NONE (조용한 폴백)")
    void loadMissingChatReturnsNone() {
        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        AlarmDraft loaded = store.load(99L);

        assertThat(loaded.getIntent()).isEqualTo(AlarmIntent.NONE);
    }

    @Test
    @DisplayName("존재하지 않는 chatId save → CustomException (드러내는 실패)")
    void saveMissingChatThrows() {
        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> store.save(99L, AlarmDraft.none()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("깨진 JSON 컬럼 load → NONE (디시리얼라이즈 실패 폴백)")
    void corruptJsonReturnsNone() {
        Chat chat = Chat.builder().childId(7L).build();
        chat.updatePendingAlarmDraft("{ this is not json");
        when(chatRepository.findById(4L)).thenReturn(Optional.of(chat));

        AlarmDraft loaded = store.load(4L);

        assertThat(loaded.getIntent()).isEqualTo(AlarmIntent.NONE);
    }
}
