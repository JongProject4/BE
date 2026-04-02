package com.aikids.care.domain.user.service;

import com.aikids.care.domain.user.dto.GoogleUserInfoDto;
import com.aikids.care.domain.user.model.SocialType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserService userService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

		String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUserNameAttributeName();

		Map<String, Object> attributes = oauth2User.getAttributes();
		GoogleUserInfoDto googleUserInfo = GoogleUserInfoDto.from(attributes, userNameAttributeName);

		if (googleUserInfo.getSocialId() == null || googleUserInfo.getSocialId().isBlank()) {
			throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_user"), "socialId is missing");
		}

		var savedUser = userService.upsertSocialUser(
				googleUserInfo.getSocialId(),
				socialType,
				googleUserInfo.getName()
		);

		// 컨트롤러에서 local user를 조회할 때 쓰도록 추가 속성을 실어 반환
		Map<String, Object> augmentedAttributes = new HashMap<>(attributes);
		augmentedAttributes.put("socialId", savedUser.getSocialId());
		augmentedAttributes.put("socialType", savedUser.getSocialType().name());
		augmentedAttributes.put("userId", savedUser.getId());
		augmentedAttributes.put("name", savedUser.getName());

		return new DefaultOAuth2User(
				oauth2User.getAuthorities(),
				augmentedAttributes,
				userNameAttributeName
		);
	}
}

