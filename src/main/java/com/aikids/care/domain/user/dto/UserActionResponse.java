package com.aikids.care.domain.user.dto;

public record UserActionResponse(
		boolean success,
		String message
) {
	public static UserActionResponse success(String message) {
		return new UserActionResponse(true, message);
	}

	public static UserActionResponse fail(String message) {
		return new UserActionResponse(false, message);
	}
}
