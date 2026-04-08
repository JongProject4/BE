package com.aikids.care.domain.child.service;

import com.aikids.care.domain.child.dto.ChildResponse;
import com.aikids.care.domain.child.dto.CreateChildRequest;
import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildService {

	private final ChildRepository childRepository;
	private final UserRepository userRepository;

	@Transactional
	public ChildResponse createChild(String socialId, SocialType socialType, CreateChildRequest request) {
		if (socialId == null || socialId.isBlank()) {
			throw new IllegalArgumentException("socialId must not be blank");
		}
		if (socialType == null) {
			throw new IllegalArgumentException("socialType must not be null");
		}
		if (request == null) {
			throw new IllegalArgumentException("request must not be null");
		}
		if (request.name() == null || request.name().trim().isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (request.birthdate() == null) {
			throw new IllegalArgumentException("birthdate must not be null");
		}
		if (request.gender() == null) {
			throw new IllegalArgumentException("gender must not be null");
		}

		User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseThrow(() -> new EntityNotFoundException("User not found. socialId=" + socialId));

		// 인증된 보호자(User) 아래에 Child를 생성한다.
		Child child = Child.builder()
				.user(user)
				.name(request.name().trim())
				.birthdate(request.birthdate())
				.gender(request.gender())
				.height(request.height())
				.weight(request.weight())
				.medicalHistory(request.medicalHistory())
				.allergies(request.allergies())
				.build();

		return ChildResponse.from(childRepository.save(child));
	}

	@Transactional(readOnly = true)
	public List<ChildResponse> getChildren(String socialId, SocialType socialType) {
		if (socialId == null || socialId.isBlank()) {
			throw new IllegalArgumentException("socialId must not be blank");
		}
		if (socialType == null) {
			throw new IllegalArgumentException("socialType must not be null");
		}

		User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseThrow(() -> new EntityNotFoundException("User not found. socialId=" + socialId));

		return childRepository.findByUser_Id(user.getId())
				.stream()
				.map(ChildResponse::from)
				.toList();
	}
}
