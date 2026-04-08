package com.aikids.care.domain.user.dto;

public record PatchUserInfoRequest(
		String name,
		String phoneNumber,
		String fcmToken
) {
	public boolean isEmpty() {
		boolean nameEmpty = name == null || name.trim().isBlank();
		boolean phoneEmpty = phoneNumber == null || phoneNumber.trim().isBlank();
		boolean fcmEmpty = fcmToken == null || fcmToken.trim().isBlank();
		return nameEmpty && phoneEmpty && fcmEmpty;
	}
}
