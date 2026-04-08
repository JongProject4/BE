package com.aikids.care.global.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String COOKIE_NAME = "oauth2_authorization_request";
	private static final int COOKIE_MAX_AGE_SECONDS = 300;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
		if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
			return null;
		}
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
			Object deserialized = SerializationUtils.deserialize(decoded);
			if (deserialized instanceof OAuth2AuthorizationRequest authorizationRequest) {
				return authorizationRequest;
			}
			log.warn("OAuth2 authorization request cookie type mismatch: {}", deserialized == null ? "null" : deserialized.getClass().getName());
			return null;
		} catch (Exception ex) {
			log.warn("Failed to load OAuth2AuthorizationRequest from cookie", ex);
			return null;
		}
	}

	@Override
	public void saveAuthorizationRequest(
			OAuth2AuthorizationRequest authorizationRequest,
			HttpServletRequest request,
			HttpServletResponse response) {
		if (authorizationRequest == null) {
			deleteCookie(request, response);
			return;
		}
		try {
			byte[] serialized = SerializationUtils.serialize(authorizationRequest);
			if (serialized == null || serialized.length == 0) {
				throw new IllegalStateException("Serialized OAuth2AuthorizationRequest is empty");
			}
			String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(serialized);
			Cookie cookie = new Cookie(COOKIE_NAME, encoded);
			cookie.setPath("/");
			cookie.setHttpOnly(true);
			cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
			cookie.setSecure(request.isSecure());
			response.addCookie(cookie);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to persist OAuth2AuthorizationRequest", ex);
		}
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
		OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
		deleteCookie(request, response);
		return loaded;
	}

	private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(COOKIE_NAME, null);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0);
		cookie.setSecure(request.isSecure());
		response.addCookie(cookie);
	}
}
