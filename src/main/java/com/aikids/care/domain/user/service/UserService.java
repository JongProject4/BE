package com.aikids.care.domain.user.service;

import com.aikids.care.domain.user.dto.CurrentUserResponse;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public User upsertSocialUser(String socialId, SocialType socialType, String name) {
		if (socialId == null || socialId.isBlank()) {
			throw new IllegalArgumentException("socialId must not be blank");
		}
		if (socialType == null) {
			throw new IllegalArgumentException("socialType must not be null");
		}

		User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseGet(() -> User.builder()
						.socialId(socialId)
						.socialType(socialType)
						.name(name)
						.build());

		// 기존 유저면 (예: 이름 변경) 정도만 반영
		user.updateName(name);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse getCurrentUser(String socialId, SocialType socialType) {
		if (socialId == null || socialId.isBlank()) {
			throw new IllegalArgumentException("socialId must not be blank");
		}
		if (socialType == null) {
			throw new IllegalArgumentException("socialType must not be null");
		}

		User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseThrow(() -> new EntityNotFoundException("User not found. socialId=" + socialId));

		return CurrentUserResponse.from(user);
	}
}

