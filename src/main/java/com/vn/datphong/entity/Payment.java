package com.vn.datphong.entity;

import com.vn.datphong.enums.PaymentMethodType;
import com.vn.datphong.enums.PaymentProvider;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.persistence.PaymentMethodConverter;

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
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Một booking có thể có nhiều lần thử (FAILED rồi thanh toán lại); chỉ một payment "đang hoạt động"
    // tại một thời điểm, bảo đảm bởi partial unique index uq_payments_one_active_per_booking.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentMethodType method = PaymentMethodType.FAKE_BANK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.FAKE_BANK;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatusType status = PaymentStatusType.PENDING;

    /** Tổng đã hoàn cho payment này; luôn <= amount. */
    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "captured_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal capturedAmount = BigDecimal.ZERO;

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean open = true;

    @Column(name = "merchant_id", nullable = false)
    @Builder.Default
    private String merchantId = "datphong";

    @Column(name = "intent_expires_at", nullable = false)
    private Instant intentExpiresAt;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    /** Số tài khoản Fake Bank dùng để trả (chỉ là mã giả lập, không phải thẻ/tài khoản thật). */
    @Column(name = "payer_account", length = 40)
    private String payerAccount;

    @Column(name = "gateway_transaction_ref", length = 100)
    private String gatewayTransactionRef;

    @Column(name = "provider_reference", unique = true, length = 120)
    private String providerReference;

    @Column(name = "idempotency_key", length = 120, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Version
    private Long version;

    @PrePersist
    private void initializeIntent() {
        if (correlationId == null) {
            correlationId = org.slf4j.MDC.get("traceId");
            if (correlationId == null) correlationId = UUID.randomUUID().toString();
        }
        if (intentExpiresAt == null) {
            intentExpiresAt = booking.getExpiresAt() == null ? Instant.now().plusSeconds(900)
                    : booking.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        }
        updateOpenState();
    }

    @PreUpdate
    private void updateOpenState() {
        open = status == PaymentStatusType.PENDING || status == PaymentStatusType.INITIATED
                || status == PaymentStatusType.PROCESSING || status == PaymentStatusType.TIMED_OUT
                || status == PaymentStatusType.UNKNOWN || status == PaymentStatusType.RECONCILING;
    }
}
