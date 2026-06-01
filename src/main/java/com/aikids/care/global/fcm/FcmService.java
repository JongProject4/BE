package com.aikids.care.global.fcm;

import com.aikids.care.domain.user.service.UserDeviceService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseApp.class)
public class FcmService {

    private final UserDeviceService userDeviceService;

    public void sendToUser(Long userId, String title, String body) {
        List<String> tokens = userDeviceService.getTokensByUserId(userId);
        if (tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            try {
                FirebaseMessaging.getInstance().send(buildMessage(token, title, body));
            } catch (FirebaseMessagingException e) {
                if (isInvalidToken(e)) {
                    log.info("FCM 토큰 만료, 삭제: userId={}", userId);
                    userDeviceService.removeDevice(token);
                } else {
                    log.warn("FCM 발송 실패: userId={}, code={}", userId, e.getMessagingErrorCode(), e);
                }
            }
        }
    }

    private Message buildMessage(String token, String title, String body) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
