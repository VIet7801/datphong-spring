package com.vn.datphong.entity;

import com.vn.datphong.enums.MailEventType;
import com.vn.datphong.enums.MailStatus;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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
@Table(name = "mail_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mail_events_0", columnNames = {"event_id", "event_type", "recipient_identity"}),
    @UniqueConstraint(name = "uq_mail_events_1", columnNames = {"event_type", "business_key", "recipient_identity", "template_version_id"})
})
public class MailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private MailEventType eventType;

    @Column(name = "business_key", nullable = false)
    private String businessKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recipient_id", nullable = true)
    private User recipient;

    @Column(name = "recipient_identity", nullable = false)
    private String recipientIdentity;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Column(name = "recipient_masked", nullable = false)
    private String recipientMasked;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false)
    private EmailTemplateVersion templateVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MailStatus status = MailStatus.QUEUED;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @Column(name = "expires_at", nullable = true)
    private Instant expiresAt;

    @Column(name = "lease_owner", nullable = true)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = true)
    private Instant leaseUntil;

    @Column(name = "last_error_code", nullable = true)
    private String lastErrorCode;

    @Column(name = "dead_letter_reason", nullable = true, columnDefinition = "TEXT")
    private String deadLetterReason;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
