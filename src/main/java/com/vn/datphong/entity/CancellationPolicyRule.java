package com.vn.datphong.entity;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cancellation_policy_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cancellationpolicyrule_0", columnNames = {"policy_id", "days_before_checkin"})
})
public class CancellationPolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CancellationPolicy policy;

    @Column(name = "days_before_checkin", nullable = false)
    private Integer daysBeforeCheckin;

    @Column(name = "refund_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercent;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;
}
