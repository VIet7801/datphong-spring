package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fakebank_webhook_events")
public class FakeBankWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Cố ý không unique: kịch bản DUPLICATE gửi hai lần cùng eventId để thử bảo vệ replay phía nhận. */
    @Column(name = "event_id", nullable = false, length = 60)
    private String eventId;
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;
    @Column(nullable = false, length = 60)
    private String reference;
    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 5)
    private String currency;
    @Column(name = "failure_code", length = 60)
    private String failureCode;
    /** SENT khi bên nhận trả thành công. */
    @Column(nullable = false, length = 10)
    private String status = "PENDING";
    @Column(nullable = false)
    private Integer attempts = 0;
    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;
    @Column(name = "last_error", length = 300)
    private String lastError;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}