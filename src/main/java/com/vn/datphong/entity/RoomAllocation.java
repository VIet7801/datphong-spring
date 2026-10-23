package com.vn.datphong.entity;

import com.vn.datphong.enums.AllocationStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;
import com.vn.datphong.persistence.StayRange;
import com.vn.datphong.persistence.StayRangeType;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;

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
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Type(StayRangeType.class)
    @Column(name = "stay", nullable = false, columnDefinition = "daterange")
    private StayRange stay;

    @Formula("lower(stay)")
    private LocalDate checkinDate;

    @Formula("upper(stay)")
    private LocalDate checkoutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.HELD;

    @Version
    private Long version;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason", length = 40)
    private String releaseReason;

    @PrePersist
    @PreUpdate
    private void updateStay() {
        if (checkinDate != null && checkoutDate != null) stay = new StayRange(checkinDate, checkoutDate);
        if (stay == null) throw new IllegalArgumentException("Cần khoảng ngày giữ phòng hợp lệ");
    }
}

