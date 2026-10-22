package com.vn.datphong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Clock dùng chung (dễ thay trong test) và bật @Scheduled cho job hết hạn, outbox, đối soát. */
@Configuration
@EnableScheduling
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
