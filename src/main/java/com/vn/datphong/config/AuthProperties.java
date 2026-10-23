package com.vn.datphong.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private Duration verificationTtl = Duration.ofMinutes(20);
    private int verificationMaxAttempts = 5;
    private Duration resendCooldown = Duration.ofMinutes(1);
    private Duration refreshTtl = Duration.ofDays(7);
    private Duration rateWindow = Duration.ofMinutes(5);
    private int ipLimit = 50;
    private int accountLimit = 10;
    private String ratePrefix = "datphong:auth:";
    private String mailFrom = "no-reply@datphong.local";
    private String mailLocale = "vi-VN";
}
