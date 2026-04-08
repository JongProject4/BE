package com.aikids.care.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		// OAuth2 로그인 시작/콜백 경로는 JWT 검사 대상에서 제외한다.
		return path.startsWith("/login")
				|| path.startsWith("/oauth2")
				|| path.startsWith("/error");
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
			// 1) 토큰이 유효하지 않으면 즉시 401로 종료한다.
			if (!jwtTokenProvider.validateToken(rawToken)) {
				response.sendError(HttpStatus.UNAUTHORIZED.value());
				return;
			}
			// 2) 유효한 토큰이면 Authentication을 구성해 SecurityContext에 저장한다.
			Authentication authentication = jwtTokenProvider.getAuthentication(rawToken);
			if (authentication instanceof AbstractAuthenticationToken tokenAuthentication) {
				tokenAuthentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			}
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException | IllegalArgumentException ex) {
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return;
		}

		filterChain.doFilter(request, response);
	}
}
