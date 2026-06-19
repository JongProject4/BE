package com.aikids.care.domain.chat.alarm;

import com.aikids.care.domain.hospitalalarm.service.HospitalAlarmService;
import com.aikids.care.domain.medicationalarm.service.MedicationAlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlarmFlowHandlerTest {

    private static final Long CHAT_ID = 100L;
    private static final Long USER_ID = 10L;
    private static final Long CHILD_ID = 7L;

    private AlarmIntentExtractor extractor;
    private PendingAlarmDraftStore draftStore;
    private MedicationAlarmService medicationAlarmService;
    private HospitalAlarmService hospitalAlarmService;
    private AlarmFlowHandler handler;

    @BeforeEach
    void setUp() {
        extractor = mock(AlarmIntentExtractor.class);
        draftStore = mock(PendingAlarmDraftStore.class);
        medicationAlarmService = mock(MedicationAlarmService.class);
        hospitalAlarmService = mock(HospitalAlarmService.class);
        handler = new AlarmFlowHandler(extractor, draftStore, medicationAlarmService, hospitalAlarmService);
    }

    @Test
    @DisplayName("진행 중 초안 없음 + 알람 의도 없음 → 빈 Optional (일반 상담으로 폴스루)")
    void noIntentFallsThrough() {
        when(draftStore.load(CHAT_ID)).thenReturn(AlarmDraft.none());
        when(extractor.extract(anyString(), any())).thenReturn(AlarmDraft.none());

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "아이가 열이 나요");

        assertThat(reply).isEmpty();
        verify(draftStore, never()).save(anyLong(), any());
    }

    @Test
    @DisplayName("새 초안 + 슬롯 완비 → 초안 저장 + 확인 프롬프트")
    void newCompleteDraftAsksConfirm() {
        when(draftStore.load(CHAT_ID)).thenReturn(AlarmDraft.none());
        AlarmDraft extracted = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .dosage("5ml")
                .intervalHour(24)
                .build();
        when(extractor.extract(anyString(), any())).thenReturn(extracted);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "타이레놀 5ml 24시간마다 알림");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("타이레놀", "5ml", "24");
        assertThat(reply.get()).contains("등록할게요");
        verify(draftStore).save(eq(CHAT_ID), eq(extracted));
        verify(medicationAlarmService, never()).register(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("새 초안 + 슬롯 누락 → 초안 저장 + 누락 슬롯 질문")
    void newIncompleteDraftAsksForMissing() {
        when(draftStore.load(CHAT_ID)).thenReturn(AlarmDraft.none());
        AlarmDraft extracted = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .build();
        when(extractor.extract(anyString(), any())).thenReturn(extracted);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "타이레놀 알림 등록해줘");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("복용량", "복용 간격");
        verify(draftStore).save(eq(CHAT_ID), eq(extracted));
    }

    @Test
    @DisplayName("진행 중 초안 + CONFIRM + 슬롯 완비 → 실제 등록 + clear + 완료 응답")
    void pendingConfirmCompleteRegisters() {
        AlarmDraft pending = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .dosage("5ml")
                .intervalHour(24)
                .build();
        when(draftStore.load(CHAT_ID)).thenReturn(pending);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "네");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("타이레놀", "5ml", "24");
        verify(medicationAlarmService).register(CHILD_ID, USER_ID, "타이레놀", "5ml", 24);
        verify(draftStore).clear(CHAT_ID);
        verify(extractor, never()).extract(anyString(), any());
    }

    @Test
    @DisplayName("진행 중 내원 초안 + CONFIRM → HospitalAlarmService.register 호출")
    void pendingConfirmHospitalRegisters() {
        LocalDateTime visit = LocalDateTime.of(2026, 6, 25, 14, 0);
        AlarmDraft pending = AlarmDraft.builder()
                .intent(AlarmIntent.HOSPITAL)
                .hospitalName("서울아이병원")
                .visitDate(visit)
                .memo("정기검진")
                .build();
        when(draftStore.load(CHAT_ID)).thenReturn(pending);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "네");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("서울아이병원");
        verify(hospitalAlarmService).register(CHILD_ID, USER_ID, "서울아이병원", visit, "정기검진");
        verify(draftStore).clear(CHAT_ID);
    }

    @Test
    @DisplayName("진행 중 초안 + CONFIRM이지만 슬롯 누락 → 등록 안 함, 다시 질문")
    void pendingConfirmIncompleteAsksAgain() {
        AlarmDraft pending = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .build();
        when(draftStore.load(CHAT_ID)).thenReturn(pending);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "네");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("복용량");
        verify(medicationAlarmService, never()).register(anyLong(), anyLong(), anyString(), anyString(), any());
        verify(draftStore, never()).clear(anyLong());
    }

    @Test
    @DisplayName("진행 중 초안 + CANCEL → clear + 취소 응답")
    void pendingCancelClears() {
        AlarmDraft pending = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .build();
        when(draftStore.load(CHAT_ID)).thenReturn(pending);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "취소할래");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("취소");
        verify(draftStore).clear(CHAT_ID);
        verify(medicationAlarmService, never()).register(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("진행 중 초안 + AMBIGUOUS 슬롯 보강 → merge + save + 적절한 응답")
    void pendingAmbiguousFillsSlotsAndMerges() {
        AlarmDraft pending = AlarmDraft.builder()
                .intent(AlarmIntent.MEDICATION)
                .medicineName("타이레놀")
                .build();
        when(draftStore.load(CHAT_ID)).thenReturn(pending);

        AlarmDraft incoming = AlarmDraft.builder()
                .dosage("5ml")
                .intervalHour(12)
                .build();
        when(extractor.extract(anyString(), eq(pending))).thenReturn(incoming);

        Optional<String> reply = handler.handle(CHAT_ID, USER_ID, CHILD_ID, "5ml씩 12시간마다");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("타이레놀", "5ml", "12");
        verify(draftStore).save(eq(CHAT_ID), any(AlarmDraft.class));
        verify(medicationAlarmService, never()).register(anyLong(), anyLong(), anyString(), anyString(), any());
    }
}
