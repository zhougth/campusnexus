package com.campusnexus.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return true;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(key(jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
