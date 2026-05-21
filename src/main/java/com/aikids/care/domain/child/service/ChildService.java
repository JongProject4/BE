package com.aikids.care.domain.child.service;

import com.aikids.care.domain.child.dto.ChildResponse;
import com.aikids.care.domain.child.dto.CreateChildRequest;
import com.aikids.care.domain.child.dto.PatchChildRequest;
import com.aikids.care.domain.child.entity.Child;
import com.aikids.care.domain.child.repository.ChildRepository;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;
import com.aikids.care.domain.user.model.UserRepository;
import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
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
		if (request.name() == null || request.name().trim().isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (request.birthdate() == null) {
			throw new IllegalArgumentException("birthdate must not be null");
		}
		if (request.gender() == null) {
			throw new IllegalArgumentException("gender must not be null");
		}

		User user = findUser(socialId, socialType);

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
		User user = findUser(socialId, socialType);
		return childRepository.findByUser_Id(user.getId())
				.stream()
				.map(ChildResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ChildResponse getChild(String socialId, SocialType socialType, Long childId) {
		User user = findUser(socialId, socialType);
		Child child = childRepository.findByIdAndUser_Id(childId, user.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
		return ChildResponse.from(child);
	}

	@Transactional
	public void deleteChild(String socialId, SocialType socialType, Long childId) {
		User user = findUser(socialId, socialType);
		Child child = childRepository.findByIdAndUser_Id(childId, user.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));
		childRepository.delete(child);
	}

	@Transactional
	public ChildResponse patchChild(String socialId, SocialType socialType, Long childId, PatchChildRequest request) {
		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("At least one field must be provided");
		}

		User user = findUser(socialId, socialType);
		Child child = childRepository.findByIdAndUser_Id(childId, user.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.CHILD_NOT_FOUND));

		child.patchProfile(
				request.name(),
				request.birthdate(),
				request.gender(),
				request.height(),
				request.weight(),
				request.medicalHistory(),
				request.allergies()
		);

		return ChildResponse.from(childRepository.save(child));
	}

	private User findUser(String socialId, SocialType socialType) {
		return userRepository.findBySocialIdAndSocialType(socialId, socialType)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}
