package com.vn.datphong.config;

import com.vn.datphong.security.TokenSecrets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakeBankConfig {
    /** Bộ giả lập và bên nhận cùng dùng một khóa; có thể cấu hình khóa cố định qua môi trường. */
    @Bean("fakeBankWebhookSecret")
    public String webhookSecret(@Value("${app.fakebank.webhook-secret:}") String configured) {
        return configured.isBlank() ? TokenSecrets.create() : configured;
    }
}
