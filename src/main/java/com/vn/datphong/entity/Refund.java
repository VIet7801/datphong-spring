package com.vn.datphong.entity;

import com.vn.datphong.enums.PaymentProvider;
import com.vn.datphong.enums.RefundStatusType;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refunds", uniqueConstraints = {
    @UniqueConstraint(name = "uq_refunds_0", columnNames = {"refund_code"}),
    @UniqueConstraint(name = "uq_refunds_1", columnNames = {"payment_id", "idempotency_key"}),
    @UniqueConstraint(name = "uq_refunds_2", columnNames = {"provider", "merchant_id", "provider_reference"})
})
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "refund_code", nullable = false)
    private String refundCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "provider_reference", nullable = true)
    private String providerReference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RefundStatusType status = RefundStatusType.REQUESTED;

    @Column(name = "reservation_active", nullable = false)
    @Builder.Default
    private Boolean reservationActive = true;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "requested_by_id", nullable = true)
    private User requestedBy;

    @Column(name = "processed_at", nullable = true)
    private Instant processedAt;

    @Column(name = "failure_code", nullable = true)
    private String failureCode;

    @Column(name = "failure_message", nullable = true, columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "next_attempt_at", nullable = true)
    private Instant nextAttemptAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Version
    private Long version;

}
