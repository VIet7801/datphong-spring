package com.vn.datphong.entity;

import com.vn.datphong.enums.ReviewStatusType;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chỉ cho phép đánh giá sau khi booking đã "completed" - kiểm tra ở tầng service
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReviewStatusType status = ReviewStatusType.PUBLISHED;

    @Column(name = "host_reply", nullable = true)
    private String hostReply;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "host_replied_by_id", nullable = true)
    private User hostRepliedBy;

    @Column(name = "host_replied_at", nullable = true)
    private Instant hostRepliedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "moderated_by_id", nullable = true)
    private User moderatedBy;

    @Column(name = "moderated_at", nullable = true)
    private Instant moderatedAt;

    @Column(name = "moderation_reason", nullable = true)
    private String moderationReason;

    @Version
    private Long version;
}
