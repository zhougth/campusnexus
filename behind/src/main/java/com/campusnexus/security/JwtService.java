package com.campusnexus.security;

import com.campusnexus.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofHours(properties.getExpireHours()));
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public JwtPayload parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer version = claims.get("ver", Integer.class);
        return new JwtPayload(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.getId(),
                version == null ? 0 : version,
                claims.getExpiration().toInstant()
        );
    }

    public long remainingSeconds(JwtPayload payload) {
        return Math.max(0, Duration.between(Instant.now(), payload.expiration()).getSeconds());
    }
}
