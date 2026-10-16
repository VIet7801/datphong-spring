package com.vn.datphong.entity;

import com.vn.datphong.enums.BankEventType;
import com.vn.datphong.enums.InboxStatus;
import com.vn.datphong.enums.PaymentProvider;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bank_webhook_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_bank_webhook_events_0", columnNames = {"provider", "merchant_id", "event_id"})
})
public class BankWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private BankEventType eventType;

    @Column(name = "provider_reference", nullable = false)
    private String providerReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "refund_id", nullable = true)
    private Refund refund;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "provider_occurred_at", nullable = true)
    private Instant providerOccurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = true)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    @Builder.Default
    private InboxStatus processingStatus = InboxStatus.RECEIVED;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = true)
    private Instant nextAttemptAt;

    @Column(name = "lease_owner", nullable = true)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = true)
    private Instant leaseUntil;

    @Column(name = "last_error", nullable = true, columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
