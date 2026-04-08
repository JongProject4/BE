package com.aikids.care.global.security.oauth2;

import com.aikids.care.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final OAuth2FrontendProperties oauth2FrontendProperties;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected authentication type");
			return;
		}

		OAuth2User oauth2User = oauth2Authentication.getPrincipal();
		Map<String, Object> attrs = oauth2User.getAttributes();
		String socialId = (String) attrs.get("socialId");
		String socialTypeStr = (String) attrs.get("socialType");
		String name = (String) attrs.get("name");
		if (!StringUtils.hasText(socialId) || !StringUtils.hasText(socialTypeStr)) {
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "OAuth2 attributes are missing social info.");
			return;
		}

		String jwt = jwtTokenProvider.createAccessToken(socialId, socialTypeStr, name);

		String redirectBase = oauth2FrontendProperties.frontendRedirectUri();
		if (!StringUtils.hasText(redirectBase)) {
			redirectBase = "http://localhost:8080/login-success";
		}

		String targetUrl = UriComponentsBuilder.fromUriString(redirectBase)
				.queryParam("token", jwt)
				.build()
				.toUriString();

		redirectStrategy.sendRedirect(request, response, targetUrl);
	}
}
