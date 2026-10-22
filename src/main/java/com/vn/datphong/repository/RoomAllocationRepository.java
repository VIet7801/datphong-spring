package com.vn.datphong.repository;

import com.vn.datphong.entity.RoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long> {
    @Query("select a from RoomAllocation a where a.booking.id = :bookingId")
    Optional<RoomAllocation> findByBookingId(@Param("bookingId") Long bookingId);

    /** Số allocation còn hiệu lực giao [checkIn, checkOut) của phòng. Hold hết hạn (chưa được job giải phóng) không tính. */
    @Query("""
            select count(a) from RoomAllocation a
            where a.room.id = :roomId and a.releasedAt is null
              and (a.holdExpiresAt is null or a.holdExpiresAt > :now)
              and a.checkinDate < :checkOut and a.checkoutDate > :checkIn
            """)
    long countActiveOverlap(@Param("roomId") Long roomId, @Param("checkIn") LocalDate checkIn,
                            @Param("checkOut") LocalDate checkOut, @Param("now") LocalDateTime now);

    /** Booking có hold đã hết hạn nhưng allocation chưa được giải phóng: được giải phóng "lười" khi có booking mới. */
    @Query("""
            select a.booking.id from RoomAllocation a
            where a.room.id = :roomId and a.releasedAt is null
              and a.holdExpiresAt is not null and a.holdExpiresAt <= :now
              and a.checkinDate < :checkOut and a.checkoutDate > :checkIn
            order by a.booking.id
            """)
    List<Long> findStaleBookingIds(@Param("roomId") Long roomId, @Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut, @Param("now") LocalDateTime now);

    @Query("""
            select count(a) from RoomAllocation a
            where a.room.id = :roomId and a.releasedAt is null and a.checkoutDate > :today
            """)
    long countFutureForRoom(@Param("roomId") Long roomId, @Param("today") LocalDate today);

    @Query("""
            select count(a) from RoomAllocation a
            where a.room.property.id = :propertyId and a.releasedAt is null and a.checkoutDate > :today
            """)
    long countFutureForProperty(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    @Query("""
            select count(a) from RoomAllocation a
            where a.room.roomTypeEntity.id = :roomTypeId and a.releasedAt is null
              and a.checkinDate < :to and a.checkoutDate > :from
            """)
    long countActiveForRoomType(@Param("roomTypeId") Long roomTypeId, @Param("from") LocalDate from,
                                @Param("to") LocalDate to);
}