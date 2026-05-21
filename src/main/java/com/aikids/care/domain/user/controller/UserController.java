package com.aikids.care.domain.user.controller;

import com.aikids.care.domain.user.dto.CurrentUserResponse;
import com.aikids.care.domain.user.dto.FcmTokenRequest;
import com.aikids.care.domain.user.dto.PatchUserInfoRequest;
import com.aikids.care.domain.user.dto.UpdateUserInfoRequest;
import com.aikids.care.domain.user.dto.UserActionResponse;
import com.aikids.care.domain.user.service.UserService;
import com.aikids.care.global.security.OAuth2Utils;
import com.aikids.care.global.security.OAuth2Utils.AuthInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal OAuth2User oauth2User) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		return userService.getCurrentUser(auth.socialId(), auth.socialType());
	}

	@PostMapping("/me")
	public ResponseEntity<UserActionResponse> updateMe(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody UpdateUserInfoRequest request
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		userService.updateCurrentUserInfo(auth.socialId(), auth.socialType(), request);
		return ResponseEntity.ok(UserActionResponse.success("User additional info updated successfully."));
	}

	@PatchMapping("/me")
	public ResponseEntity<UserActionResponse> patchMe(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody PatchUserInfoRequest request
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		userService.patchCurrentUserInfo(auth.socialId(), auth.socialType(), request);
		return ResponseEntity.ok(UserActionResponse.success("User profile patched successfully."));
	}

	@DeleteMapping("/me")
	public ResponseEntity<UserActionResponse> deleteMe(@AuthenticationPrincipal OAuth2User oauth2User) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		userService.deleteCurrentUser(auth.socialId(), auth.socialType());
		return ResponseEntity.ok(UserActionResponse.success("User account deleted successfully."));
	}

	@PatchMapping("/me/fcm-token")
	public ResponseEntity<Void> updateFcmToken(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody FcmTokenRequest request
	) {
		AuthInfo auth = OAuth2Utils.extractAuthInfo(oauth2User);
		userService.updateCurrentUserFcmToken(auth.socialId(), auth.socialType(), request.fcmToken());
		return ResponseEntity.noContent().build();
	}
}
