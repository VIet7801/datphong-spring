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

    // Một booking có thể có nhiều lần thử (FAILED rồi thanh toán lại); chỉ một payment "đang hoạt động"
    // tại một thời điểm, bảo đảm bởi partial unique index uq_payments_one_active_per_booking.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
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
}