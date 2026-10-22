package com.vn.datphong.repository;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.BookingStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);


    boolean existsByBookingCode(String bookingCode);

    /**
     * Khóa hàng booking. Thứ tự khóa toàn hệ thống: room -> booking -> payment -> allocation
     * (đặc tả mục 4.1); mọi luồng callback/hủy/hết hạn khóa booking trước payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            select b from Booking b join fetch b.room r join fetch b.property p
            where b.user.id = :userId and b.status in :statuses order by b.id desc
            """,
            countQuery = "select count(b) from Booking b where b.user.id = :userId and b.status in :statuses")
    Page<Booking> findByUser(@Param("userId") Long userId, @Param("statuses") Collection<BookingStatusType> statuses,
                             Pageable pageable);

    @Query(value = """
            select b from Booking b join fetch b.user u join fetch b.room r join fetch b.property p
            where p.id in :propertyIds and b.status in :statuses
              and b.checkinDate < :toDate and b.checkoutDate > :fromDate
            order by b.id desc
            """,
            countQuery = """
                    select count(b) from Booking b
                    where b.property.id in :propertyIds and b.status in :statuses
                      and b.checkinDate < :toDate and b.checkoutDate > :fromDate
                    """)
    Page<Booking> findForHost(@Param("propertyIds") Collection<Long> propertyIds,
                              @Param("statuses") Collection<BookingStatusType> statuses,
                              @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    /** Booking PENDING_PAYMENT đã quá hạn giữ chỗ; job kiểm tra lại trạng thái trong transaction trước khi hết hạn. */
    @Query("select b.id from Booking b where b.status = :status and b.expiresAt <= :now order by b.expiresAt")
    List<Long> findDueIds(@Param("status") BookingStatusType status, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            select count(b) from Booking b
            where b.roomType.id = :roomTypeId and b.guestsCount > :maxGuests and b.checkoutDate > :today
              and b.status in (com.vn.datphong.enums.BookingStatusType.PENDING_PAYMENT,
                               com.vn.datphong.enums.BookingStatusType.CONFIRMED,
                               com.vn.datphong.enums.BookingStatusType.CHECKED_IN)
            """)
    long countFutureGuestsAbove(@Param("roomTypeId") Long roomTypeId, @Param("maxGuests") Integer maxGuests,
                                @Param("today") LocalDate today);

    @Query("""
            select count(b) from Booking b
            where b.user.id = :userId and b.property.id = :propertyId and b.status = :status
            """)
    long countByUserPropertyStatus(@Param("userId") Long userId, @Param("propertyId") Long propertyId,
                                   @Param("status") BookingStatusType status);

    @Query("select b.property.owner.id from Booking b where b.id = :bookingId")
    Long findOwnerIdByBookingId(@Param("bookingId") Long bookingId);

    @Query("select coalesce(sum(b.totalPrice), 0) from Booking b where b.property.id in :propertyIds and b.status in :statuses and b.checkinDate >= :from and b.checkinDate < :to")
    BigDecimal sumTotal(@Param("propertyIds") Collection<Long> propertyIds,
                        @Param("statuses") Collection<BookingStatusType> statuses,
                        @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select b.status, count(b) from Booking b where b.property.id in :propertyIds and b.checkinDate >= :from and b.checkinDate < :to group by b.status")
    List<Object[]> countByStatus(@Param("propertyIds") Collection<Long> propertyIds, @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}