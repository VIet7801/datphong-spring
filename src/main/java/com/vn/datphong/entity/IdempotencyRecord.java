package com.vn.datphong.entity;

import com.vn.datphong.enums.IdempotencyStatus;

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
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"scope", "idempotency_key"})
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope", nullable = false)
    private String scope;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "resource_type", nullable = true)
    private String resourceType;

    @Column(name = "resource_id", nullable = true)
    private Long resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "actor_key", nullable = false)
    private String actorKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private IdempotencyStatus status = IdempotencyStatus.IN_PROGRESS;

    @Column(name = "response_code", nullable = true)
    private Integer responseCode;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    @Column(name = "expires_at", nullable = true)
    private Instant expiresAt;
}
