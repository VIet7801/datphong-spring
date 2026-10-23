package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.BookingStatusHistory;
import com.vn.datphong.entity.RoomAllocation;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.BookingStatusHistoryRepository;
import com.vn.datphong.repository.RoomAllocationRepository;
import com.vn.datphong.util.BookingStateMachine;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Mọi đổi trạng thái booking và giải phóng allocation đi qua đây, luôn trong transaction đã khóa booking. */
@Component
public class BookingLifecycle {
    private final BookingStatusHistoryRepository historyRepository;
    private final RoomAllocationRepository allocationRepository;
    private final Clock clock;

    public BookingLifecycle(BookingStatusHistoryRepository historyRepository,
                            RoomAllocationRepository allocationRepository, Clock clock) {
        this.historyRepository = historyRepository;
        this.allocationRepository = allocationRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void transition(Booking booking, BookingStatusType to, BookingStatusChangeSource source, User actor, String reason) {
        BookingStatusType from = booking.getStatus();
        if (from == to) {
            return;
        }
        if (!BookingStateMachine.canTransition(from, to)) {
            throw DomainException.conflict("INVALID_STATE", "Không thể chuyển booking từ " + from + " sang " + to);
        }
        booking.setStatus(to);
        historyRepository.save(BookingStatusHistory.builder()
                .booking(booking).fromStatus(from).toStatus(to).source(source).actor(actor)
                .reason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)))
                .correlationId(MDC.get("traceId"))
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseAllocation(Long bookingId, String reason) {
        allocationRepository.findByBookingId(bookingId).ifPresent(a -> release(a, reason));
    }

    private void release(RoomAllocation a, String reason) {
        if (a.getReleasedAt() == null) {
            a.setReleasedAt(LocalDateTime.now(clock));
            a.setReleaseReason(reason);
            a.setHoldExpiresAt(null);
        }
    }

    /** Xác nhận thanh toán: allocation trở thành vĩnh viễn (không còn hạn giữ). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void pinAllocation(Long bookingId) {
        allocationRepository.findByBookingId(bookingId).ifPresent(a -> a.setHoldExpiresAt(null));
    }
}
