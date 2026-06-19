package com.aikids.care.domain.chat.alarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlarmIntentExtractorTest {

    private ChatModel chatModel;
    private AlarmIntentExtractor extractor;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        extractor = new AlarmIntentExtractor(chatModel, mapper);
    }

    @Nested
    @DisplayName("휴리스틱 사전 필터")
    class HeuristicFilter {

        @Test
        @DisplayName("알람 키워드 없는 일반 상담은 LLM 호출 없이 NONE")
        void skipsLlmForNonAlarmMessage() {
            AlarmDraft result = extractor.extract("아이가 열이 38도 정도 나요", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.NONE);
            verify(chatModel, never()).call(anyString());
        }

        @Test
        @DisplayName("빈/공백 메시지는 NONE")
        void blankMessageReturnsNone() {
            assertThat(extractor.extract("", null).getIntent()).isEqualTo(AlarmIntent.NONE);
            assertThat(extractor.extract("   ", null).getIntent()).isEqualTo(AlarmIntent.NONE);
            assertThat(extractor.extract(null, null).getIntent()).isEqualTo(AlarmIntent.NONE);
            verify(chatModel, never()).call(anyString());
        }

        @Test
        @DisplayName("진행 중 초안이 있으면 키워드 매칭 없어도 LLM 호출")
        void invokesLlmWhenPendingDraftExists() {
            AlarmDraft pending = AlarmDraft.builder()
                    .intent(AlarmIntent.MEDICATION)
                    .medicineName("타이레놀")
                    .build();
            when(chatModel.call(anyString())).thenReturn(
                    "{\"intent\":\"MEDICATION\",\"dosage\":\"5ml\",\"intervalHour\":12}");

            AlarmDraft result = extractor.extract("5ml씩 12시간마다", pending);

            assertThat(result.getDosage()).isEqualTo("5ml");
            assertThat(result.getIntervalHour()).isEqualTo(12);
            verify(chatModel).call(anyString());
        }
    }

    @Nested
    @DisplayName("LLM 응답 파싱")
    class LlmResponseParsing {

        @Test
        @DisplayName("복약 의도 전체 슬롯")
        void medicationFullSlots() {
            when(chatModel.call(anyString())).thenReturn(
                    "{\"intent\":\"MEDICATION\",\"medicineName\":\"타이레놀\",\"dosage\":\"5ml\",\"intervalHour\":24}");

            AlarmDraft result = extractor.extract("타이레놀 5ml 24시간마다 알려줘", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.MEDICATION);
            assertThat(result.getMedicineName()).isEqualTo("타이레놀");
            assertThat(result.getDosage()).isEqualTo("5ml");
            assertThat(result.getIntervalHour()).isEqualTo(24);
            assertThat(result.isComplete()).isTrue();
        }

        @Test
        @DisplayName("내원 의도 visitDate ISO 파싱")
        void hospitalVisitDateParsing() {
            when(chatModel.call(anyString())).thenReturn(
                    "{\"intent\":\"HOSPITAL\",\"hospitalName\":\"서울아이병원\",\"visitDate\":\"2026-06-25T14:00:00\"}");

            AlarmDraft result = extractor.extract("다음주 화요일 오후 2시 서울아이병원 예약", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.HOSPITAL);
            assertThat(result.getHospitalName()).isEqualTo("서울아이병원");
            assertThat(result.getVisitDate()).isEqualTo(LocalDateTime.of(2026, 6, 25, 14, 0));
        }

        @Test
        @DisplayName("마크다운 코드블록 감싸진 JSON 파싱")
        void markdownCodeBlockStripped() {
            when(chatModel.call(anyString())).thenReturn("""
                    ```json
                    {"intent":"MEDICATION","medicineName":"타이레놀","dosage":"5ml","intervalHour":24}
                    ```
                    """);

            AlarmDraft result = extractor.extract("타이레놀 5ml 24시간마다 알려줘", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.MEDICATION);
            assertThat(result.getMedicineName()).isEqualTo("타이레놀");
        }

        @Test
        @DisplayName("JSON 아닌 응답은 NONE")
        void invalidJsonReturnsNone() {
            when(chatModel.call(anyString())).thenReturn("이건 JSON이 아닙니다");

            AlarmDraft result = extractor.extract("타이레놀 알림 등록해줘", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.NONE);
        }

        @Test
        @DisplayName("LLM 호출 예외 시 NONE — 일반 상담 흐름은 깨지지 않음")
        void llmExceptionReturnsNone() {
            when(chatModel.call(anyString())).thenThrow(new RuntimeException("API down"));

            AlarmDraft result = extractor.extract("타이레놀 알림 등록해줘", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.NONE);
        }
    }

    @Nested
    @DisplayName("슬롯 누락 / 미완 상태")
    class IncompleteSlots {

        @Test
        @DisplayName("복약 의도 + dosage 누락")
        void medicationMissingDosage() {
            when(chatModel.call(anyString())).thenReturn(
                    "{\"intent\":\"MEDICATION\",\"medicineName\":\"타이레놀\",\"dosage\":null,\"intervalHour\":24}");

            AlarmDraft result = extractor.extract("타이레놀 24시간마다 알려줘", null);

            assertThat(result.getIntent()).isEqualTo(AlarmIntent.MEDICATION);
            assertThat(result.isComplete()).isFalse();
            assertThat(result.missingFields()).containsExactly("dosage");
        }

        @Test
        @DisplayName("내원 의도 + visitDate 누락")
        void hospitalMissingVisitDate() {
            when(chatModel.call(anyString())).thenReturn(
                    "{\"intent\":\"HOSPITAL\",\"hospitalName\":\"서울아이병원\",\"visitDate\":null}");

            AlarmDraft result = extractor.extract("서울아이병원 예약 알림 등록", null);

            assertThat(result.isComplete()).isFalse();
            assertThat(result.missingFields()).containsExactly("visitDate");
        }
    }
}
