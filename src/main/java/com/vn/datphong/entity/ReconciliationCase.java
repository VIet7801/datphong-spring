package com.vn.datphong.entity;

import com.vn.datphong.enums.ReconciliationStatus;
import com.vn.datphong.enums.ReconciliationType;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "reconciliation_cases", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reconciliation_cases_0", columnNames = {"operation_key"})
})
public class ReconciliationCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "refund_id", nullable = true)
    private Refund refund;

    @Column(name = "operation_key", nullable = false)
    private String operationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private ReconciliationType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.OPEN;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> detail;

    @Column(name = "next_attempt_at", nullable = true)
    private Instant nextAttemptAt;

    @Column(name = "resolved_at", nullable = true)
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "assigned_to_id", nullable = true)
    private User assignedTo;

    @Column(name = "resolution", nullable = true, columnDefinition = "TEXT")
    private String resolution;

    @Version
    private Long version;

}
