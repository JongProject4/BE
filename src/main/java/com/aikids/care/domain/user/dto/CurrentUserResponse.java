package com.aikids.care.domain.user.dto;

import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.model.User;

public record CurrentUserResponse(
		Long id,
		String socialId,
		SocialType socialType,
		String name,
		String phoneNumber
) {
	public static CurrentUserResponse from(User user) {
		return new CurrentUserResponse(
				user.getId(),
				user.getSocialId(),
				user.getSocialType(),
				user.getName(),
				user.getPhoneNumber()
		);
	}
}
