package com.huace.trace.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, String userType) {
        return generateToken(userId, username, userType, null, null);
    }

    public String generateToken(Long userId, String username, String userType, Long enterpriseId, String accountLevel) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(expiryDate);
        if (enterpriseId != null) builder.claim("enterpriseId", enterpriseId);
        if (accountLevel != null) builder.claim("accountLevel", accountLevel);
        return builder.signWith(getSigningKey()).compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getPayload().getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).getPayload().get("userId", Long.class);
    }

    public String getUserTypeFromToken(String token) {
        return getClaims(token).getPayload().get("userType", String.class);
    }

    public Long getEnterpriseIdFromToken(String token) {
        return getClaims(token).getPayload().get("enterpriseId", Long.class);
    }

    public String getAccountLevelFromToken(String token) {
        return getClaims(token).getPayload().get("accountLevel", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }
}
