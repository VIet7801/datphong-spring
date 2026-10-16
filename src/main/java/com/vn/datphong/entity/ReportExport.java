package com.vn.datphong.entity;

import com.vn.datphong.enums.ReportStatus;

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
@Table(name = "report_exports", uniqueConstraints = {
    @UniqueConstraint(name = "uq_report_exports_0", columnNames = {"requested_by_id", "request_key"})
})
public class ReportExport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(name = "request_key", nullable = false)
    private String requestKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> scopeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> querySnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.QUEUED;

    @Column(name = "as_of", nullable = true)
    private Instant asOf;

    @Column(name = "object_key", nullable = true)
    private String objectKey;

    @Column(name = "row_count", nullable = true)
    private Long rowCount;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "finished_at", nullable = true)
    private Instant finishedAt;

    @Column(name = "expires_at", nullable = true)
    private Instant expiresAt;

    @Column(name = "failure_code", nullable = true)
    private String failureCode;

    @Column(name = "lease_owner", nullable = true)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = true)
    private Instant leaseUntil;

    @Version
    private Long version;

}
