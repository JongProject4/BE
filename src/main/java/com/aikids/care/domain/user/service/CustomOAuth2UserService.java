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
		// 0) 기본 OAuth2UserService로 provider(구글 등)에서 사용자 정보를 조회한다.
		OAuth2User oauth2User = super.loadUser(userRequest);

		// 1) 어떤 소셜 로그인인지 식별한다. (google, kakao ...)
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

		// 2) provider별 고유 식별자 키를 가져온다. (구글은 보통 "sub")
		String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUserNameAttributeName();

		// 3) provider가 내려준 원본 attributes에서 우리 서비스가 필요한 정보(socialId, name)를 추출한다.
		Map<String, Object> attributes = oauth2User.getAttributes();
		GoogleUserInfoDto googleUserInfo = GoogleUserInfoDto.from(attributes, userNameAttributeName);

		// 4) socialId가 비어 있으면 사용자 식별이 불가능하므로 인증 예외를 발생시킨다.
		if (googleUserInfo.getSocialId() == null || googleUserInfo.getSocialId().isBlank()) {
			throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_user"), "socialId is missing");
		}

		// 5) 로컬 DB에 소셜 유저를 생성/갱신(upsert)한다.
		var savedUser = userService.upsertSocialUser(
				googleUserInfo.getSocialId(),
				socialType,
				googleUserInfo.getName()
		);

		// 6) 이후 API에서 @AuthenticationPrincipal로 로컬 유저를 식별할 수 있도록
		//    socialId/socialType/userId/name을 attributes에 보강해서 반환한다.
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

