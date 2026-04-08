package com.aikids.care.global.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String COOKIE_NAME = "oauth2_authorization_request";
	private static final int COOKIE_MAX_AGE_SECONDS = 300;

	private final ObjectMapper objectMapper;

	public CookieOAuth2AuthorizationRequestRepository() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new OAuth2ClientJackson2Module());
		this.objectMapper.findAndRegisterModules();
	}

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
		if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
			return null;
		}
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
			return objectMapper.readValue(decoded, OAuth2AuthorizationRequest.class);
		} catch (Exception ex) {
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
			byte[] json = objectMapper.writeValueAsBytes(authorizationRequest);
			String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json);
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
