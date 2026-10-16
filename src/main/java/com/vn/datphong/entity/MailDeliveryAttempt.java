package com.vn.datphong.entity;

import com.vn.datphong.enums.MailAttemptResult;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mail_delivery_attempts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mail_delivery_attempts_0", columnNames = {"mail_event_id", "attempt_no"})
})
public class MailDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mail_event_id", nullable = false)
    private MailEvent mailEvent;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "operator_action_key", nullable = true)
    private String operatorActionKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "triggered_by_id", nullable = true)
    private User triggeredBy;

    @Column(name = "provider_message_id", nullable = true)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private MailAttemptResult result;

    @Column(name = "error_code", nullable = true)
    private String errorCode;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
