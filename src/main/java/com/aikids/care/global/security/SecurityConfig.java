package com.aikids.care.global.security;

import com.aikids.care.domain.user.service.CustomOAuth2UserService;
import com.aikids.care.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll()
				)
				.oauth2Login(oauth2 -> oauth2
						.successHandler(oAuth2AuthenticationSuccessHandler)
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				);

		return http.build();
	}
}

