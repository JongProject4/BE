package com.aikids.care;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class TokenGenerator {

    public static void main(String[] args) {
        String secret = "testSecretKey1234567890abcdefghijklmn";
        long expirationMs = 86400000L; // 24시간

        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Long userId = 1L; // 테스트용 userId (실제 DB에 존재하는 값으로 교체)

        String token = Jwts.builder()
                .subject("testUser")
                .issuedAt(now)
                .expiration(expiry)
                .claim("socialId", "testUser")
                .claim("socialType", "KAKAO")
                .claim("userId", userId)
                .claim("name", "테스트")
                .signWith(secretKey)
                .compact();

        System.out.println("Token: " + token);
    }
}