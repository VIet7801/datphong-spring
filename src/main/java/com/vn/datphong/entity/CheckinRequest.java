package com.vn.datphong.entity;

import com.vn.datphong.enums.CheckinRequestStatus;

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
@Table(name = "checkin_requests", uniqueConstraints = {
    @UniqueConstraint(name = "uq_checkin_requests_0", columnNames = {"origin_message_id"})
})
public class CheckinRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_message_id", nullable = false)
    private Message originMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "counter_proposed_at", nullable = true)
    private Instant counterProposedAt;

    @Column(name = "agreed_checkin_at", nullable = true)
    private Instant agreedCheckinAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CheckinRequestStatus status = CheckinRequestStatus.PENDING;

    @Column(name = "note", nullable = true, columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responded_by_id", nullable = true)
    private User respondedBy;

    @Column(name = "responded_at", nullable = true)
    private Instant respondedAt;

    @Column(name = "response_note", nullable = true, columnDefinition = "TEXT")
    private String responseNote;

    @Column(name = "customer_accepted_at", nullable = true)
    private Instant customerAcceptedAt;

    @Column(name = "closed_at", nullable = true)
    private Instant closedAt;

    @Version
    private Long version;

}
