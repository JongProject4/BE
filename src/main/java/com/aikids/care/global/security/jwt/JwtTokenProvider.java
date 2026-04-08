package com.aikids.care.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
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
}
