package com.vn.datphong.entity;

import com.vn.datphong.enums.ActorType;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;

import java.time.*;
import java.util.*;

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
@Table(name = "booking_status_history")
public class BookingStatusHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    @Builder.Default
    private ActorType actorType = ActorType.SYSTEM;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private BookingStatusType fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private BookingStatusType toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatusChangeSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(length = 500)
    private String reason;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void identifyEvent() {
        if (actor != null) actorType = ActorType.USER;
        if (correlationId == null) correlationId = UUID.randomUUID().toString();
    }
}
