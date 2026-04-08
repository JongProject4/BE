package com.aikids.care.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(header)) {
			filterChain.doFilter(request, response);
			return;
		}

		String trimmed = header.trim();
		if (!trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			filterChain.doFilter(request, response);
			return;
		}

		String rawToken = trimmed.substring(BEARER_PREFIX.length()).trim();
		while (rawToken.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			rawToken = rawToken.substring(BEARER_PREFIX.length()).trim();
		}
		if (!StringUtils.hasText(rawToken)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return;
		}

		try {
			var claims = jwtTokenProvider.parseAndValidate(rawToken);
			String socialId = claims.get("socialId", String.class);
			String socialType = claims.get("socialType", String.class);
			String name = claims.get("name", String.class);
			if (!StringUtils.hasText(socialId) || !StringUtils.hasText(socialType)) {
				response.sendError(HttpStatus.UNAUTHORIZED.value());
				return;
			}

			Map<String, Object> attributes = new HashMap<>();
			attributes.put("socialId", socialId);
			attributes.put("socialType", socialType);
			attributes.put("name", name == null ? "" : name);

			OAuth2User oauth2User = new DefaultOAuth2User(
					List.of(new SimpleGrantedAuthority("ROLE_USER")),
					attributes,
					"socialId");

			var authentication = new OAuth2AuthenticationToken(
					oauth2User,
					oauth2User.getAuthorities(),
					"jwt");
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException | IllegalArgumentException ex) {
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return;
		}

		filterChain.doFilter(request, response);
	}
}
