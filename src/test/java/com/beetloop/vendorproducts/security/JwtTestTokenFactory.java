package com.beetloop.vendorproducts.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class JwtTestTokenFactory {

    public static final String TEST_SECRET = "01234567890123456789012345678901";

    private JwtTestTokenFactory() {
    }

    public static String bearerToken(String userId, List<String> roles) {
        return "Bearer " + accessToken(userId, roles);
    }

    public static String accessToken(String userId, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }
}
