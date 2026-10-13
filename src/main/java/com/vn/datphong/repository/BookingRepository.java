package com.vn.datphong.repository;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.checkinDate < :checkoutDate AND b.checkoutDate > :startDate")
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("startDate") LocalDate startDate,
                          @Param("checkoutDate") LocalDate checkoutDate);
}


