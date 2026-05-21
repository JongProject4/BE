package com.aikids.care.global.security;

import com.aikids.care.domain.user.model.SocialType;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2Utils {

    public static AuthInfo extractAuthInfo(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("Unauthenticated user.");
        }
        Map<String, Object> attributes = oauth2User.getAttributes();
        String socialId = (String) attributes.get("socialId");
        String socialTypeStr = (String) attributes.get("socialType");
        if (socialId == null || socialId.isBlank() || socialTypeStr == null || socialTypeStr.isBlank()) {
            throw new IllegalStateException("OAuth2 attributes are missing social info.");
        }
        return new AuthInfo(socialId, SocialType.valueOf(socialTypeStr));
    }

    public static Long extractUserId(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("Unauthenticated user.");
        }
        Object userId = oauth2User.getAttributes().get("userId");
        if (userId == null) {
            throw new IllegalStateException("OAuth2 attributes are missing userId.");
        }
        return ((Number) userId).longValue();
    }

    public record AuthInfo(String socialId, SocialType socialType) {}
}
