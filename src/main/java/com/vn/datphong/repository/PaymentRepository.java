package com.vn.datphong.repository;

import com.vn.datphong.entity.Payment;
import com.vn.datphong.enums.PaymentStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    Optional<Payment> findByProviderReference(String providerReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Payment p where p.booking.id = :bookingId order by p.id desc")
    List<Payment> findByBooking(@Param("bookingId") Long bookingId);

    @Query("select p from Payment p where p.booking.id = :bookingId and p.status in :statuses order by p.id desc")
    List<Payment> findByBookingAndStatuses(@Param("bookingId") Long bookingId,
                                           @Param("statuses") Collection<PaymentStatusType> statuses);

    @Query("select p.id from Payment p where p.status in :statuses and p.updatedAt <= :before order by p.id")
    List<Long> findStaleIds(@Param("statuses") Collection<PaymentStatusType> statuses,
                            @Param("before") LocalDateTime before, Pageable pageable);

    /** Tiền thực thu còn lại của booking: đã thu trừ đã hoàn, chỉ tính payment SUCCEEDED. */
    @Query("select coalesce(sum(p.amount - p.refundedAmount), 0) from Payment p where p.booking.id = :bookingId and p.status = com.vn.datphong.enums.PaymentStatusType.SUCCEEDED")
    BigDecimal netCaptured(@Param("bookingId") Long bookingId);

    /** Scalar projection: không nạp entity vào persistence context để lần khóa sau đọc dữ liệu mới nhất. */
    @Query("select p.booking.id from Payment p where p.id = :id")
    Optional<Long> findBookingId(@Param("id") Long id);
}
