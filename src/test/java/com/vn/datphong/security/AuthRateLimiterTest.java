package com.vn.datphong.security;

import com.vn.datphong.config.AuthProperties;
import com.vn.datphong.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthRateLimiterTest {
    @Test
    void redisFailureFailsClosedWithoutExposingConnectionDetails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("private connection details"));
        DomainException error = assertThrows(DomainException.class,
                () -> new AuthRateLimiter(redis, new AuthProperties()).check("login", "user@example.test", "127.0.0.1"));
        assertEquals(503, error.getStatus().value());
        assertEquals("AUTH_TEMPORARILY_UNAVAILABLE", error.getCode());
        assertFalse(error.getMessage().contains("private"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DATPHONG_RBAC_INTEGRATION", matches = "true")
    void concurrentRequestsUseOneAtomicCounterWithExpiryInRealRedis() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yaml"));
        var connection = new RedisStandaloneConfiguration(
                UserRbacIntegrationTest.TestDatabase.setting(config, "SPRING_DATA_REDIS_HOST"),
                Integer.parseInt(UserRbacIntegrationTest.TestDatabase.setting(config, "SPRING_DATA_REDIS_PORT")));
        connection.setPassword(UserRbacIntegrationTest.TestDatabase.setting(config, "SPRING_DATA_REDIS_PASSWORD"));
        connection.setDatabase(Integer.parseInt(
                UserRbacIntegrationTest.TestDatabase.setting(config, "SPRING_DATA_REDIS_DATABASE")));
        var factory = new LettuceConnectionFactory(connection);
        factory.afterPropertiesSet();
        factory.start();
        StringRedisTemplate redis = new StringRedisTemplate(factory);
        AuthProperties properties = new AuthProperties();
        properties.setRatePrefix("junit:auth:" + UUID.randomUUID() + ":");
        properties.setAccountLimit(5);
        properties.setIpLimit(100);
        String email = "rate@example.test";
        String ip = "127.0.0.1";
        List<String> keys = List.of(properties.getRatePrefix() + "login:ip:" + TokenSecrets.hash(ip),
                properties.getRatePrefix() + "login:account:" + TokenSecrets.hash(email));
        var limiter = new AuthRateLimiter(redis, properties);
        var pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Integer>> requests = java.util.stream.IntStream.range(0, 12)
                    .mapToObj(i -> (Callable<Integer>) () -> {
                        try {
                            limiter.check("login", email, ip);
                            return 200;
                        } catch (DomainException error) {
                            return error.getStatus().value();
                        }
                    }).toList();
            int allowed = 0;
            for (Future<Integer> result : pool.invokeAll(requests)) {
                int status = result.get(10, TimeUnit.SECONDS);
                assertTrue(status == 200 || status == 429);
                if (status == 200) allowed++;
            }
            assertEquals(5, allowed);
            for (String key : keys) {
                assertTrue(redis.getExpire(key) > 0);
                assertFalse(key.contains(email));
            }
            assertEquals("12", redis.opsForValue().get(keys.get(1)));
        } finally {
            pool.shutdownNow();
            redis.delete(keys);
            factory.destroy();
        }
    }
}
