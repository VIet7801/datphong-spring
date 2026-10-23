package com.vn.datphong.security;

import com.vn.datphong.config.AuthProperties;
import com.vn.datphong.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthRateLimiter {
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public void check(String action, String identity, String ip) {
        checkBucket(action + ":ip:", ip, properties.getIpLimit());
        checkBucket(action + ":account:", identity, properties.getAccountLimit());
    }

    private void checkBucket(String bucket, String value, int limit) {
        Long count;
        try {
            count = redis.execute(INCREMENT,
                    List.of(properties.getRatePrefix() + bucket + TokenSecrets.hash(value)),
                    Long.toString(Math.max(1, properties.getRateWindow().toSeconds())));
        } catch (DataAccessException failure) {
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_TEMPORARILY_UNAVAILABLE",
                    "Chưa thể xử lý xác thực, hãy thử lại sau");
        }
        if (count == null) {
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_TEMPORARILY_UNAVAILABLE",
                    "Chưa thể xử lý xác thực, hãy thử lại sau");
        }
        if (count > limit) {
            throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Quá nhiều yêu cầu, hãy thử lại sau");
        }
    }
}
