package org.intern.shopeefoodclone.infras.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void set(String key, String value, long durationSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(durationSeconds));
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure");
            return true;
        }
    }
}
