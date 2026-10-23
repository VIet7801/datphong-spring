package com.vn.datphong.repository;

import com.vn.datphong.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    Optional<BookingStatusHistory> findFirstByBooking_IdOrderByIdDesc(Long bookingId);

    @Query("select h from BookingStatusHistory h where h.booking.id = :bookingId order by h.id")
    List<BookingStatusHistory> findByBooking(@Param("bookingId") Long bookingId);
}
