package com.aikids.care.domain.user.dto;

import java.util.Map;

public class GoogleUserInfoDto {

	private final String socialId;
	private final String name;

	public GoogleUserInfoDto(String socialId, String name) {
		this.socialId = socialId;
		this.name = name;
	}

	public String getSocialId() {
		return socialId;
	}

	public String getName() {
		return name;
	}

	public static GoogleUserInfoDto from(Map<String, Object> attributes, String userNameAttributeName) {
		Object socialIdObj = attributes.get(userNameAttributeName); // Usually: "sub"
		String socialId = socialIdObj == null ? null : socialIdObj.toString();

		String name = (String) attributes.get("name");
		if (name == null || name.isBlank()) {
			String givenName = (String) attributes.get("given_name");
			String familyName = (String) attributes.get("family_name");
			name = ((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName)).trim();
		}

		if (name == null || name.isBlank()) {
			name = socialId; // last resort; avoids persisting null
		}

		return new GoogleUserInfoDto(socialId, name);
	}
}

