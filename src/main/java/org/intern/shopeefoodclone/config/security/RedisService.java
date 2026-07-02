package org.intern.shopeefoodclone.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String tokenId, long expirationDurationSeconds) {
        redisTemplate.opsForValue().set("blacklisted:" + tokenId, "true", expirationDurationSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted:" + tokenId));
    }
}
