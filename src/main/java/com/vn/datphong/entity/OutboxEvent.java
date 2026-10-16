package com.vn.datphong.entity;

import com.vn.datphong.enums.OutboxStatus;

import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "outbox_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_outboxevent_0", columnNames = {"event_id"})
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at", nullable = true)
    private Instant processedAt;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "payload_version", nullable = false)
    @Builder.Default
    private Integer payloadVersion = 1;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "lease_owner", nullable = true)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = true)
    private Instant leaseUntil;

    @Column(name = "last_error", nullable = true, columnDefinition = "TEXT")
    private String lastError;
}
