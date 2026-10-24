package com.campusnexus.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisCacheHelper {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T get(String key, TypeReference<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            redisTemplate.delete(key);   // 兼容旧格式/脏数据，删掉当 miss
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }
}
