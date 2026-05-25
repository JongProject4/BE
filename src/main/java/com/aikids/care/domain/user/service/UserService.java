package com.aikids.care.domain.user.service;

import com.aikids.care.domain.user.dto.CurrentUserResponse;
import com.aikids.care.domain.user.dto.PatchUserInfoRequest;
import com.aikids.care.domain.user.dto.UpdateUserInfoRequest;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public User upsertSocialUser(String socialId, SocialType socialType, String name) {
		User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseGet(() -> User.builder()
						.socialId(socialId)
						.socialType(socialType)
						.name(name)
						.build());
		user.updateName(name);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse getCurrentUser(String socialId, SocialType socialType) {
		return CurrentUserResponse.from(findUser(socialId, socialType));
	}

	@Transactional
	public void updateCurrentUserInfo(String socialId, SocialType socialType, UpdateUserInfoRequest request) {
		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("At least one field (phoneNumber, fcmToken) must be provided");
		}
		User user = findUser(socialId, socialType);
		user.updateAdditionalInfo(request.phoneNumber(), request.fcmToken());
		userRepository.save(user);
	}

	@Transactional
	public void patchCurrentUserInfo(String socialId, SocialType socialType, PatchUserInfoRequest request) {
		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("At least one field (name, phoneNumber, fcmToken) must be provided");
		}
		User user = findUser(socialId, socialType);
		user.updateName(request.name());
		user.updateAdditionalInfo(request.phoneNumber(), request.fcmToken());
		userRepository.save(user);
	}

	@Transactional
	public void deleteCurrentUser(String socialId, SocialType socialType) {
		userRepository.delete(findUser(socialId, socialType));
	}

	@Transactional
	public void updateCurrentUserFcmToken(String socialId, SocialType socialType, String fcmToken) {
		if (fcmToken == null || fcmToken.trim().isBlank()) {
			throw new IllegalArgumentException("fcmToken must not be blank");
		}
		String normalizedToken = fcmToken.trim();
		User user = findUser(socialId, socialType);
		if (normalizedToken.equals(user.getFcmToken())) {
			return;
		}
		user.updateFcmToken(normalizedToken);
		userRepository.save(user);
	}

	private User findUser(String socialId, SocialType socialType) {
		return userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}
