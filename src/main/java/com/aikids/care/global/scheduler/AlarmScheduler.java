package com.aikids.care.global.scheduler;

import com.aikids.care.domain.hospitalalarm.entity.HospitalAlarm;
import com.aikids.care.domain.hospitalalarm.repository.HospitalAlarmRepository;
import com.aikids.care.domain.medicationalarm.entity.MedicationAlarm;
import com.aikids.care.domain.medicationalarm.repository.MedicationAlarmRepository;
import com.aikids.care.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(FcmService.class)
public class AlarmScheduler {

    private final MedicationAlarmRepository medicationAlarmRepository;
    private final HospitalAlarmRepository hospitalAlarmRepository;
    private final FcmService fcmService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void sendMedicationAlarms() {
        LocalDateTime now = LocalDateTime.now();
        List<MedicationAlarm> alarms = medicationAlarmRepository.findAlarmsToNotify(now);
        for (MedicationAlarm alarm : alarms) {
            Long userId = alarm.getChild().getUser().getId();
            String childName = alarm.getChild().getName();
            fcmService.sendToUser(userId,
                    "복약 알림",
                    childName + " 복약 시간이에요: " + alarm.getMedicineName() + " " + alarm.getDosage());
            alarm.updateNextNotifyAt(now.plusHours(alarm.getIntervalHour()));
        }
        if (!alarms.isEmpty()) {
            log.info("복약 알림 {}건 발송 완료", alarms.size());
        }
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void sendHospitalAlarms() {
        LocalDateTime now = LocalDateTime.now();
        sendHospitalStage(now, 7 * 24 * 60, HospitalAlarm.STAGE_7D, "7일 후 병원 방문이 있어요");
        sendHospitalStage(now, 3 * 24 * 60, HospitalAlarm.STAGE_3D, "3일 후 병원 방문이 있어요");
        sendHospitalStage(now, 24 * 60,     HospitalAlarm.STAGE_1D, "내일 병원 방문이 있어요");
        sendHospitalStage(now, 60,          HospitalAlarm.STAGE_1H, "1시간 후 병원 방문이 있어요");
    }

    private void sendHospitalStage(LocalDateTime now, int minutesBefore, int stageBit, String message) {
        LocalDateTime threshold = now.plusMinutes(minutesBefore);
        List<Long> ids = hospitalAlarmRepository.findAlarmIdsToNotify(stageBit, now, threshold);
        if (ids.isEmpty()) return;

        List<HospitalAlarm> alarms = hospitalAlarmRepository.findByIdsWithUser(ids);
        for (HospitalAlarm alarm : alarms) {
            Long userId = alarm.getChild().getUser().getId();
            String childName = alarm.getChild().getName();
            fcmService.sendToUser(userId,
                    "내원 알림",
                    childName + "의 " + alarm.getHospitalName() + " 예약: " + message);
            alarm.addNotifiedStage(stageBit);
        }
        if (!alarms.isEmpty()) {
            log.info("내원 알림 stageBit={} {}건 발송 완료", stageBit, alarms.size());
        }
    }
}
