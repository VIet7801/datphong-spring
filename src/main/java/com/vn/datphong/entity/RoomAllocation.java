package com.vn.datphong.entity;

import com.vn.datphong.enums.AllocationStatus;

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
@Table(name = "room_allocations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_room_allocations_0", columnNames = {"booking_id"})
})
public class RoomAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @org.hibernate.annotations.Type(com.vn.datphong.persistence.StayRangeType.class)
    @Column(name = "stay", nullable = false, columnDefinition = "daterange")
    private com.vn.datphong.persistence.StayRange stay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.HELD;

    @Column(name = "released_at", nullable = true)
    private Instant releasedAt;

    @Column(name = "release_reason", nullable = true)
    private String releaseReason;

    @Version
    private Long version;

}
