package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sổ cái của bank: mỗi dòng là một lần ghi nợ (DEBIT) hoặc hoàn tiền (REFUND), duy nhất theo idempotency key. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fakebank_transactions")
public class FakeBankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** DEBIT | REFUND */
    @Column(nullable = false, length = 10)
    private String type;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 160)
    private String idempotencyKey;
    @Column(name = "account_no", nullable = false, length = 40)
    private String accountNo;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 5)
    private String currency;
    /** SUCCESS | FAILED */
    @Column(nullable = false, length = 10)
    private String status;
    @Column(nullable = false, unique = true, length = 60)
    private String reference;
    /** Với REFUND: reference của giao dịch DEBIT gốc. */
    @Column(name = "related_reference", length = 60)
    private String relatedReference;
    @Column(name = "failure_code", length = 60)
    private String failureCode;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
