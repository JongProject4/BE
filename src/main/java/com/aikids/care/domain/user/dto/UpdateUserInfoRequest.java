package com.aikids.care.domain.user.dto;

public record UpdateUserInfoRequest(String phoneNumber) {
	public boolean isEmpty() {
		return phoneNumber == null || phoneNumber.trim().isBlank();
	}
}
