package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.service.PaymentTxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentTxServiceImpl implements PaymentTxService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Payment begin(User user, Long bookingId, String accountNo, String key, String requestHash) {
        var existing = paymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (!requestHash.equals(p.getRequestHash())) {
                throw DomainException.conflict("IDEMPOTENCY_CONFLICT", "Idempotency-Key đã được dùng cho yêu cầu khác");
            }
            return p;
        }
        Booking b = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> DomainException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy booking"));
        if (!b.getUser().getId().equals(user.getId())) {
            throw DomainException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy booking");
        }
        if (b.getStatus() != BookingStatusType.PENDING_PAYMENT) {
            throw DomainException.conflict("BOOKING_NOT_PAYABLE", "Booking ở trạng thái " + b.getStatus() + " không thể thanh toán");
        }
        if (b.getExpiresAt() != null && !b.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw DomainException.conflict("BOOKING_EXPIRED", "Booking đã hết hạn giữ chỗ");
        }
        var active = paymentRepository.findByBookingAndStatuses(bookingId, ACTIVE);
        if (!active.isEmpty()) {
            throw DomainException.conflict("PAYMENT_ALREADY_ACTIVE", "Booking đã có thanh toán đang hoạt động",
                    java.util.Map.of("paymentId", active.get(0).getId(), "status", active.get(0).getStatus().name()));
        }
        Payment p = Payment.builder().booking(b).amount(b.getTotalPrice()).currency(b.getCurrency())
                .status(PaymentStatusType.INITIATED).payerAccount(accountNo).idempotencyKey(key).requestHash(requestHash)
                .build();
        return paymentRepository.saveAndFlush(p);
    }

    @Override
    @Transactional
    public void markProcessing(Long paymentId, String reference) {
        Payment p = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        if (p.getStatus() == PaymentStatusType.INITIATED || p.getStatus() == PaymentStatusType.TIMED_OUT) {
            p.setStatus(PaymentStatusType.PROCESSING);
            p.setGatewayTransactionRef(reference);
        }
    }

    @Override
    @Transactional
    public void markTimedOut(Long paymentId) {
        Payment p = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        if (p.getStatus() == PaymentStatusType.INITIATED) {
            p.setStatus(PaymentStatusType.TIMED_OUT);
            p.setFailureCode("BANK_TIMEOUT");
        }
    }
}
