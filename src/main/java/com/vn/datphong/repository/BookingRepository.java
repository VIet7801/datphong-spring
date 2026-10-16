package com.vn.datphong.repository;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'HOLD', 'PENDING_PAYMENT') " +
            "AND b.checkinDate < :checkOut AND b.checkoutDate > :checkIn")
    long countOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    Optional<Booking> findByBookingCode(String bookingCode);
}


