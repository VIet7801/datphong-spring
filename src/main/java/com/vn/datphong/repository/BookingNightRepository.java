package com.vn.datphong.repository;

import com.vn.datphong.entity.BookingNight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingNightRepository extends JpaRepository<BookingNight, Long> {
    @Query("select n from BookingNight n where n.booking.id = :bookingId order by n.stayDate")
    List<BookingNight> findByBooking(@Param("bookingId") Long bookingId);
}

