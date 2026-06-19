package com.aikids.care.domain.chat.alarm;

import com.aikids.care.domain.healthlog.entity.HealthLog.LogType;
import com.aikids.care.domain.healthlog.service.HealthLogService;
import com.aikids.care.domain.hospitalalarm.service.HospitalAlarmService;
import com.aikids.care.domain.medicationalarm.service.MedicationAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmFlowHandler {

    private static final DateTimeFormatter VISIT_DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AlarmIntentExtractor extractor;
    private final PendingAlarmDraftStore draftStore;
    private final MedicationAlarmService medicationAlarmService;
    private final HospitalAlarmService hospitalAlarmService;
    private final HealthLogService healthLogService;

    /**
     * 알람 등록 흐름을 시도한다. 처리되면 사용자에게 보낼 응답 텍스트를 반환하고,
     * 알람 의도가 아니라고 판단되면 빈 Optional을 반환해 일반 상담 흐름으로 폴스루.
     */
    public Optional<String> handle(Long chatId, Long userId, Long childId, String userMessage) {
        AlarmDraft pending = draftStore.load(chatId);
        boolean hasPending = pending.getIntent() != null && pending.getIntent() != AlarmIntent.NONE;
        return hasPending
                ? handlePending(chatId, userId, childId, userMessage, pending)
                : handleNew(chatId, userMessage);
    }

    private Optional<String> handlePending(Long chatId, Long userId, Long childId, String userMessage, AlarmDraft pending) {
        AlarmConfirmationParser.Decision decision = AlarmConfirmationParser.parse(userMessage);

        if (decision == AlarmConfirmationParser.Decision.CANCEL) {
            draftStore.clear(chatId);
            return Optional.of("알람 등록을 취소했어요.");
        }

        if (decision == AlarmConfirmationParser.Decision.CONFIRM) {
            if (!pending.isComplete()) {
                return Optional.of(askForMissing(pending));
            }
            register(pending, userId, childId);
            draftStore.clear(chatId);
            return Optional.of(registeredReply(pending));
        }

        // AMBIGUOUS — 슬롯을 더 채우는 발화일 수 있으므로 LLM에 다시 추출 요청
        AlarmDraft incoming = extractor.extract(userMessage, pending);
        AlarmDraft merged = pending.mergeWith(incoming);
        draftStore.save(chatId, merged);
        return Optional.of(merged.isComplete() ? confirmPrompt(merged) : askForMissing(merged));
    }

    private Optional<String> handleNew(Long chatId, String userMessage) {
        AlarmDraft incoming = extractor.extract(userMessage, null);
        if (incoming.getIntent() == null || incoming.getIntent() == AlarmIntent.NONE) {
            return Optional.empty();
        }
        draftStore.save(chatId, incoming);
        return Optional.of(incoming.isComplete() ? confirmPrompt(incoming) : askForMissing(incoming));
    }

    private void register(AlarmDraft draft, Long userId, Long childId) {
        if (draft.getIntent() == AlarmIntent.MEDICATION) {
            medicationAlarmService.register(childId, userId,
                    draft.getMedicineName(), draft.getDosage(), draft.getIntervalHour());
            // 캘린더는 health_log 테이블을 조회하므로 동기 기록. eventDate=지금(KST)
            healthLogService.register(childId, userId, LogType.MEDICATION,
                    draft.getMedicineName(), toStorage(LocalDateTime.now(KST)));
            log.info("[AlarmFlow] medication registered childId={}, name={}", childId, draft.getMedicineName());
        } else {
            // visitDate는 LLM이 KST wall-clock으로 추출. JDBC가 INSERT 시 +9 더하는 버그 보상 위해
            // 저장 직전 UTC wall-clock으로 변환 → DB에 KST 의도값이 그대로 들어가도록 함.
            LocalDateTime visitForStorage = toStorage(draft.getVisitDate());
            hospitalAlarmService.register(childId, userId,
                    draft.getHospitalName(), visitForStorage, draft.getMemo());
            healthLogService.register(childId, userId, LogType.HOSPITAL,
                    draft.getHospitalName(), visitForStorage);
            log.info("[AlarmFlow] hospital registered childId={}, name={}", childId, draft.getHospitalName());
        }
    }

    // KST wall-clock LocalDateTime → UTC wall-clock LocalDateTime.
    // JDBC URL의 serverTimezone=Asia/Seoul + JVM UTC 조합이 INSERT 시 +9h를 더하는 동작을 보상.
    private LocalDateTime toStorage(LocalDateTime kstLdt) {
        return kstLdt.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private String confirmPrompt(AlarmDraft draft) {
        if (draft.getIntent() == AlarmIntent.MEDICATION) {
            return String.format("%s %s씩 %d시간마다 알려드릴까요? '네'라고 답해주시면 등록할게요.",
                    draft.getMedicineName(), draft.getDosage(), draft.getIntervalHour());
        }
        return String.format("%s %s 방문 알림으로 등록할까요? '네'라고 답해주시면 등록할게요.",
                draft.getHospitalName(),
                draft.getVisitDate().format(VISIT_DATE_FORMAT));
    }

    private String registeredReply(AlarmDraft draft) {
        if (draft.getIntent() == AlarmIntent.MEDICATION) {
            return String.format("%s %s 알람을 %d시간마다 알려드릴게요.",
                    draft.getMedicineName(), draft.getDosage(), draft.getIntervalHour());
        }
        return String.format("%s %s 방문 알림을 등록했어요.",
                draft.getHospitalName(),
                draft.getVisitDate().format(VISIT_DATE_FORMAT));
    }

    private String askForMissing(AlarmDraft draft) {
        List<String> missing = draft.missingFields();
        String labels = missing.stream().map(this::label).collect(Collectors.joining(", "));
        return labels + "을(를) 알려주시겠어요?";
    }

    private String label(String field) {
        return switch (field) {
            case "medicineName" -> "약 이름";
            case "dosage" -> "복용량";
            case "intervalHour" -> "복용 간격(시간)";
            case "hospitalName" -> "병원 이름";
            case "visitDate" -> "방문 일시";
            default -> field;
        };
    }
}
