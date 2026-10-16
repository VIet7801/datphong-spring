package com.vn.datphong.entity;

import com.vn.datphong.enums.ComplaintSeverity;
import com.vn.datphong.enums.ComplaintStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "complaint_tickets", uniqueConstraints = {
    @UniqueConstraint(name = "uq_complaint_tickets_0", columnNames = {"origin_message_id"})
})
public class ComplaintTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_message_id", nullable = false)
    private Message originMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    @Builder.Default
    private ComplaintSeverity severity = ComplaintSeverity.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "response_due_at", nullable = false)
    private Instant responseDueAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "assigned_support_id", nullable = true)
    private User assignedSupport;

    @Column(name = "acknowledged_at", nullable = true)
    private Instant acknowledgedAt;

    @Column(name = "resolution", nullable = true, columnDefinition = "TEXT")
    private String resolution;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "resolved_by_id", nullable = true)
    private User resolvedBy;

    @Column(name = "resolved_at", nullable = true)
    private Instant resolvedAt;

    @Column(name = "customer_confirmed_at", nullable = true)
    private Instant customerConfirmedAt;

    @Column(name = "closed_at", nullable = true)
    private Instant closedAt;

    @Version
    private Long version;

}
