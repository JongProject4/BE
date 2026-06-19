package com.aikids.care.domain.hospitalalarm.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalAlarmTest {

    @Nested
    @DisplayName("markPastStagesAsNotified — 등록 시점 기준 이미 지난 알림 단계 마킹")
    class MarkPastStages {

        @Test
        @DisplayName("내일 10시 알람을 오늘 19시 등록: 7D/3D/1D 마킹, 1H 미마킹")
        void registerNextDay() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 19, 19, 0);
            LocalDateTime visit = LocalDateTime.of(2026, 6, 20, 10, 0);

            HospitalAlarm alarm = newAlarm(visit);
            alarm.markPastStagesAsNotified(now);

            int stages = alarm.getNotifiedStages();
            assertThat(stages & HospitalAlarm.STAGE_7D).isNotZero();
            assertThat(stages & HospitalAlarm.STAGE_3D).isNotZero();
            assertThat(stages & HospitalAlarm.STAGE_1D).isNotZero();
            assertThat(stages & HospitalAlarm.STAGE_1H).isZero();
        }

        @Test
        @DisplayName("10일 후 알람: 어떤 단계도 마킹 안 됨 (전부 미래 트리거)")
        void registerWellAhead() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
            LocalDateTime visit = LocalDateTime.of(2026, 6, 29, 10, 0);

            HospitalAlarm alarm = newAlarm(visit);
            alarm.markPastStagesAsNotified(now);

            assertThat(alarm.getNotifiedStages()).isZero();
        }

        @Test
        @DisplayName("30분 후 알람: 모든 단계 마킹 (7D/3D/1D/1H 트리거 다 지남)")
        void registerVeryClose() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
            LocalDateTime visit = LocalDateTime.of(2026, 6, 19, 12, 30);

            HospitalAlarm alarm = newAlarm(visit);
            alarm.markPastStagesAsNotified(now);

            int expected = HospitalAlarm.STAGE_7D | HospitalAlarm.STAGE_3D
                    | HospitalAlarm.STAGE_1D | HospitalAlarm.STAGE_1H;
            assertThat(alarm.getNotifiedStages()).isEqualTo(expected);
        }

        @Test
        @DisplayName("정확히 5일 후 알람: 7D 마킹, 3D/1D/1H 미마킹")
        void registerFiveDaysAhead() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
            LocalDateTime visit = LocalDateTime.of(2026, 6, 24, 12, 0);

            HospitalAlarm alarm = newAlarm(visit);
            alarm.markPastStagesAsNotified(now);

            int stages = alarm.getNotifiedStages();
            assertThat(stages & HospitalAlarm.STAGE_7D).isNotZero();
            assertThat(stages & HospitalAlarm.STAGE_3D).isZero();
            assertThat(stages & HospitalAlarm.STAGE_1D).isZero();
            assertThat(stages & HospitalAlarm.STAGE_1H).isZero();
        }

        @Test
        @DisplayName("기존 notifiedStages 위에 OR로 추가 (덮어쓰기 안 함)")
        void preservesExistingStages() throws Exception {
            LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
            LocalDateTime visit = LocalDateTime.of(2026, 6, 29, 10, 0); // 10일 후

            HospitalAlarm alarm = newAlarm(visit);
            setField(alarm, "notifiedStages", HospitalAlarm.STAGE_7D);

            alarm.markPastStagesAsNotified(now);

            // 10일 후라 markPastStagesAsNotified가 추가하는 단계는 없어야 함.
            // 기존 STAGE_7D는 그대로 유지.
            assertThat(alarm.getNotifiedStages()).isEqualTo(HospitalAlarm.STAGE_7D);
        }
    }

    private static HospitalAlarm newAlarm(LocalDateTime visit) {
        return HospitalAlarm.builder()
                .hospitalName("테스트병원")
                .visitDate(visit)
                .memo(null)
                .build();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
