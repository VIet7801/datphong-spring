package com.vn.datphong.repository;

import com.vn.datphong.entity.RefundRequest;
import com.vn.datphong.enums.RefundStatusType;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select r from RefundRequest r where r.id = :id")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<RefundRequest> findByIdempotencyKey(String idempotencyKey);

    @Query("select r from RefundRequest r where r.booking.id = :bookingId order by r.id")
    List<RefundRequest> findByBooking(@Param("bookingId") Long bookingId);

    @Query("select r.id from RefundRequest r where r.status in :statuses and (r.nextRetryAt is null or r.nextRetryAt <= :now) order by r.id")
    List<Long> findDueIds(@Param("statuses") Collection<RefundStatusType> statuses, @Param("now") LocalDateTime now,
                          Pageable pageable);

    /** Tổng tiền đã cam kết hoàn cho payment (mọi trạng thái trừ REJECTED) để không bao giờ hoàn vượt số đã thu. */
    @Query("select coalesce(sum(r.amount), 0) from RefundRequest r where r.payment.id = :paymentId and r.status <> com.vn.datphong.enums.RefundStatusType.REJECTED")
    BigDecimal sumCommitted(@Param("paymentId") Long paymentId);

    @Query("select coalesce(sum(r.amount), 0) from RefundRequest r where r.payment.id = :paymentId and r.status = com.vn.datphong.enums.RefundStatusType.SUCCEEDED")
    BigDecimal sumSucceeded(@Param("paymentId") Long paymentId);

    /** Hai bảng refund cùng tồn tại: dữ liệu ở bảng lõi phải được đối soát trước khi chạy luồng tương thích. */
    @Query("select count(r) > 0 from Refund r where r.payment.id = :paymentId")
    boolean hasCoreRefunds(@Param("paymentId") Long paymentId);

    @Query(value = "select r from RefundRequest r where r.status in :statuses order by r.id desc",
            countQuery = "select count(r) from RefundRequest r where r.status in :statuses")
    Page<RefundRequest> findByStatuses(@Param("statuses") Collection<RefundStatusType> statuses, Pageable pageable);

    @Query("select r.booking.id from RefundRequest r where r.id = :id")
    Optional<Long> findBookingId(@Param("id") Long id);

    @Query("select r.payment.id from RefundRequest r where r.id = :id")
    Optional<Long> findPaymentId(@Param("id") Long id);
}
