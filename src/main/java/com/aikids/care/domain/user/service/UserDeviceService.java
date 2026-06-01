package com.aikids.care.domain.user.service;

import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserDevice;
import com.aikids.care.domain.user.model.UserDeviceRepository;
import com.aikids.care.domain.user.model.UserRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userDeviceRepository.findByFcmToken(fcmToken)
                .ifPresentOrElse(
                        device -> device.refreshLastSeen(user),
                        () -> userDeviceRepository.save(UserDevice.builder()
                                .user(user)
                                .fcmToken(fcmToken)
                                .build())
                );
    }

    @Transactional(readOnly = true)
    public List<String> getTokensByUserId(Long userId) {
        return userDeviceRepository.findByUser_Id(userId)
                .stream()
                .map(UserDevice::getFcmToken)
                .toList();
    }

    @Transactional
    public void removeDevice(String fcmToken) {
        userDeviceRepository.deleteByFcmToken(fcmToken);
    }
}
