package com.vn.datphong.entity;

import com.vn.datphong.enums.PaymentMethodType;
import com.vn.datphong.enums.PaymentProvider;
import com.vn.datphong.enums.PaymentStatusType;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_payment_0", columnNames = {"provider", "merchant_id", "idempotency_key"}),
    @UniqueConstraint(name = "uq_payment_1", columnNames = {"provider", "merchant_id", "provider_reference"})
})
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    @Builder.Default
    private PaymentMethodType method = PaymentMethodType.FAKE_BANK_DEBIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.FAKE_BANK;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatusType status = PaymentStatusType.INITIATED;

    @Column(name = "gateway_transaction_ref", length = 100)
    private String gatewayTransactionRef;

    @Column(name = "provider_reference", nullable = true)
    private String providerReference;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "failure_code", nullable = true)
    private String failureCode;

    @Column(name = "failure_message", nullable = true, columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "captured_at", nullable = true)
    private Instant capturedAt;

    @Version
    private Long version;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "captured_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal capturedAmount = BigDecimal.ZERO;

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    @Column(name = "intent_expires_at", nullable = false)
    private Instant intentExpiresAt;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;
}
