package com.aikids.care.global.security;

import com.aikids.care.domain.user.service.CustomOAuth2UserService;
import com.aikids.care.global.security.jwt.JwtAuthenticationFilter;
import com.aikids.care.global.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.aikids.care.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(request -> {
					var config = new org.springframework.web.cors.CorsConfiguration();
					config.setAllowedOrigins(List.of(
							"https://pediatric-ai-beige.vercel.app",
							"http://localhost:3000"
					));
					config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
					config.setAllowedHeaders(List.of("*"));
					config.setAllowCredentials(true);
					return config;
				}))
				.csrf(csrf -> csrf.disable())
				// ... 나머지 기존 코드 그대로
				// API는 세션 없이 JWT로 인증하기 위해 무상태 정책을 사용한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// /api/** 에서는 로그인 페이지 redirect 대신 401을 반환한다.
				.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll()
				)
				.oauth2Login(oauth2 -> oauth2
						// OAuth2 인가요청(state 포함)을 쿠키에 저장해 stateless 흐름을 유지한다.
						.authorizationEndpoint(authorization -> authorization
								.authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository))
						.successHandler(oAuth2AuthenticationSuccessHandler)
						// 로그인 실패 원인(예외 타입/메시지)을 로그로 남겨 원인 파악을 빠르게 한다.
						.failureHandler((request, response, exception) -> {
							log.error("OAuth2 login failed: type={}, message={}",
									exception.getClass().getName(),
									exception.getMessage(),
									exception);
							response.sendRedirect("/login?error");
						})
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				)
				// 모든 요청에서 Bearer 토큰을 먼저 검사하도록 JWT 필터를 등록한다.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}

