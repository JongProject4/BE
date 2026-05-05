package com.aikids.care.domain.user.dto;

public record UpdateUserInfoRequest(
		String phoneNumber,
		String fcmToken
) {
	public boolean isEmpty() {
		boolean phoneEmpty = phoneNumber == null || phoneNumber.trim().isBlank();
		boolean fcmEmpty = fcmToken == null || fcmToken.trim().isBlank();
		return phoneEmpty && fcmEmpty;
	}
}
