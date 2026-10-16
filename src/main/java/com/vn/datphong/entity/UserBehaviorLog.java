package com.vn.datphong.entity;

import com.vn.datphong.enums.BehaviorActionType;
import com.vn.datphong.enums.BookingSourceChannel;
import com.vn.datphong.enums.FunnelEventType;

import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_behavior_logs")
public class UserBehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private BehaviorActionType actionType;

    // Lưu tiêu chí tìm kiếm: vị trí, ngày check-in/out, số khách, bộ lọc...
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "search_query", columnDefinition = "jsonb")
    private Map<String, Object> searchQuery;

    @Column(name = "dwell_time_seconds")
    private Integer dwellTimeSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "session_key", nullable = false)
    private String sessionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private FunnelEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "property_id", nullable = true)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "room_type_id", nullable = true)
    private RoomTypeEntity roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @Column(name = "actor_key", nullable = true)
    private String actorKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false)
    private BookingSourceChannel sourceChannel;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
