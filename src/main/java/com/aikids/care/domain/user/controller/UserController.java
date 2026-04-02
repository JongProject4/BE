package com.aikids.care.domain.user.controller;

import com.aikids.care.domain.user.dto.CurrentUserResponse;
import com.aikids.care.domain.user.model.SocialType;
import com.aikids.care.domain.user.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal OAuth2User oauth2User) {
		Map<String, Object> attributes = oauth2User.getAttributes();

		String socialId = (String) attributes.get("socialId");
		String socialTypeStr = (String) attributes.get("socialType");
		if (socialId == null || socialId.isBlank() || socialTypeStr == null || socialTypeStr.isBlank()) {
			throw new IllegalStateException("OAuth2 attributes are missing social info.");
		}

		SocialType socialType = SocialType.valueOf(socialTypeStr);

		return userService.getCurrentUser(socialId, socialType);
	}
}

