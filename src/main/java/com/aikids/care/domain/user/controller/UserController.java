package com.aikids.care.domain.user.controller;

import com.aikids.care.domain.user.dto.CurrentUserResponse;
import com.aikids.care.domain.user.dto.PatchUserInfoRequest;
import com.aikids.care.domain.user.dto.UpdateUserInfoRequest;
import com.aikids.care.domain.user.dto.UserActionResponse;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
		AuthInfo authInfo = extractAuthInfo(oauth2User);
		return userService.getCurrentUser(authInfo.socialId(), authInfo.socialType());
	}

	@PostMapping("/me")
	public ResponseEntity<UserActionResponse> updateMe(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody UpdateUserInfoRequest request
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			userService.updateCurrentUserInfo(authInfo.socialId(), authInfo.socialType(), request);
			return ResponseEntity.ok(UserActionResponse.success("User additional info updated successfully."));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(UserActionResponse.fail(ex.getMessage()));
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(UserActionResponse.fail(ex.getMessage()));
		}
	}

	@PatchMapping("/me")
	public ResponseEntity<UserActionResponse> patchMe(
			@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody PatchUserInfoRequest request
	) {
		try {
			AuthInfo authInfo = extractAuthInfo(oauth2User);
			userService.patchCurrentUserInfo(authInfo.socialId(), authInfo.socialType(), request);
			return ResponseEntity.ok(UserActionResponse.success("User profile patched successfully."));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(UserActionResponse.fail(ex.getMessage()));
		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(UserActionResponse.fail(ex.getMessage()));
		}
	}

	private AuthInfo extractAuthInfo(OAuth2User oauth2User) {
		if (oauth2User == null) {
			throw new IllegalArgumentException("Unauthenticated user.");
		}
		Map<String, Object> attributes = oauth2User.getAttributes();

		String socialId = (String) attributes.get("socialId");
		String socialTypeStr = (String) attributes.get("socialType");
		if (socialId == null || socialId.isBlank() || socialTypeStr == null || socialTypeStr.isBlank()) {
			throw new IllegalStateException("OAuth2 attributes are missing social info.");
		}

		SocialType socialType = SocialType.valueOf(socialTypeStr);
		return new AuthInfo(socialId, socialType);
	}

	private record AuthInfo(String socialId, SocialType socialType) {
	}
}

