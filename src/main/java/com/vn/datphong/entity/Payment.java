package com.vn.datphong.entity;

<<<<<<< HEAD

import com.vn.datphong.enums.PaymentMethodType;
=======
import com.vn.datphong.enums.PaymentMethodType;
import com.vn.datphong.enums.PaymentProvider;
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.enums.PaymentStatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_method_type")
    private PaymentMethodType method;

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_status_type")
    @Builder.Default
    private PaymentStatusType status = PaymentStatusType.PENDING;

    @Column(name = "gateway_transaction_ref", length = 100)
    private String gatewayTransactionRef;

    @Column(name = "provider_reference", unique = true, length = 120)
    private String providerReference;

    @Column(name = "idempotency_key", length = 120)
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
