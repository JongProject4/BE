package com.aikids.care.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final int MIN_SECRET_BYTES = 32;

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
		byte[] keyBytes = decodeSecret(properties.secret());
		if (keyBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HS256");
		}
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	private static byte[] decodeSecret(String secret) {
		String trimmed = secret.trim();
		if (trimmed.startsWith("base64:")) {
			return Decoders.BASE64.decode(trimmed.substring("base64:".length()));
		}
		return trimmed.getBytes(StandardCharsets.UTF_8);
	}

	public String createAccessToken(String socialId, String socialType, String name) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + properties.expirationMs());
		return Jwts.builder()
				.subject(socialId)
				.issuedAt(now)
				.expiration(expiry)
				.claim("socialId", socialId)
				.claim("socialType", socialType)
				.claim("name", name == null ? "" : name)
				.signWith(secretKey)
				.compact();
	}

	public Claims parseAndValidate(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	/**
	 * 토큰 서명/만료를 검증한다. (실패 시 false)
	 */
	public boolean validateToken(String token) {
		try {
			parseAndValidate(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	/**
	 * JWT claims를 기반으로 Spring Security Authentication 객체를 생성한다.
	 */
	public Authentication getAuthentication(String token) {
		Claims claims = parseAndValidate(token);
		String socialId = claims.get("socialId", String.class);
		String socialType = claims.get("socialType", String.class);
		String name = claims.get("name", String.class);

		if (socialId == null || socialId.isBlank() || socialType == null || socialType.isBlank()) {
			throw new IllegalArgumentException("JWT claims are missing required social information");
		}

		Map<String, Object> attributes = Map.of(
				"socialId", socialId,
				"socialType", socialType,
				"name", name == null ? "" : name
		);
		OAuth2User oauth2User = new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				attributes,
				"socialId"
		);
		return new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "jwt");
	}
}
