package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.service.OutboxService;
import com.vn.datphong.service.PaymentOutcomeService;
import com.vn.datphong.service.RefundService;
import com.vn.datphong.util.Json;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PaymentOutcomeServiceImpl implements PaymentOutcomeService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutcomeService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingLifecycle lifecycle;
    private final RefundService refundService;
    private final OutboxService outbox;
    private final Clock clock;

    @Override
    @Transactional
    public void apply(Long paymentId, Outcome o) {
        Long bookingId = paymentRepository.findBookingId(paymentId).orElseThrow();
        Booking b = bookingRepository.findByIdForUpdate(bookingId).orElseThrow();
        Payment p = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);

        switch (p.getStatus()) {
            case SUCCEEDED, SUCCESS, REFUNDED, NEEDS_ATTENTION -> {
                if (!o.success()) {
                    log.warn("Bỏ qua kết quả THẤT BẠI muộn cho payment {} đã ở trạng thái {}", paymentId, p.getStatus());
                }
                return; // đã áp rồi: idempotent
            }
            case FAILED -> {
                if (!o.success()) {
                    return;
                }
                // Bank báo thành công sau khi ta đã ghi nhận thất bại: tiền đã bị trừ, xử lý như thanh toán trễ.
                capture(p, o, now);
                late(b, p, "Bank xác nhận thành công sau khi payment đã bị ghi FAILED");
                return;
            }
            default -> { /* PENDING, INITIATED, PROCESSING, TIMED_OUT: tiếp tục */ }
        }

        if (!o.success()) {
            p.setStatus(PaymentStatusType.FAILED);
            p.setFailureCode(o.failureCode());
            p.setFailureMessage(o.failureCode() == null ? null : "Thanh toán thất bại: " + o.failureCode());
            outbox.publish("PAYMENT_FAILED", "PAYMENT", p.getId(), Json.obj("paymentId", p.getId(),
                    "bookingId", b.getId(), "userId", b.getUser().getId(), "code", o.failureCode()));
            return;
        }

        capture(p, o, now);
        if (b.getStatus() == BookingStatusType.PENDING_PAYMENT) {
            p.setStatus(PaymentStatusType.SUCCEEDED);
            p.setPaidAt(now);
            lifecycle.transition(b, BookingStatusType.CONFIRMED, BookingStatusChangeSource.FAKE_BANK, null,
                    "Thanh toán thành công (" + o.source() + ")");
            lifecycle.pinAllocation(b.getId());
            b.setConfirmedAt(now);
            b.setExpiresAt(null);
            b.setHoldExpiresAt(null);
            outbox.publish("PAYMENT_SUCCEEDED", "PAYMENT", p.getId(), Json.obj("paymentId", p.getId(),
                    "bookingId", b.getId(), "userId", b.getUser().getId(), "amount", p.getAmount()));
            outbox.publish("BOOKING_CONFIRMED", "BOOKING", b.getId(), Json.obj("bookingId", b.getId(),
                    "bookingCode", b.getBookingCode(), "userId", b.getUser().getId()));
        } else {
            late(b, p, "Thanh toán thành công nhưng booking đang ở " + b.getStatus());
        }
    }

    private void capture(Payment p, Outcome o, LocalDateTime now) {
        p.setProviderReference(o.reference());
        p.setGatewayTransactionRef(o.reference());
        p.setCapturedAt(now);
        p.setFailureCode(null);
        p.setFailureMessage(null);
    }

    /** Saga bù trừ: tiền đã thu nhưng không còn xác nhận được booking -> đánh dấu cần xử lý và hoàn tiền toàn phần. */
    private void late(Booking b, Payment p, String why) {
        p.setStatus(PaymentStatusType.NEEDS_ATTENTION);
        if (b.getStatus() == BookingStatusType.EXPIRED) {
            lifecycle.transition(b, BookingStatusType.NEEDS_ATTENTION, BookingStatusChangeSource.FAKE_BANK, null, why);
        }
        refundService.request(b, p, p.getAmount(), RefundService.KIND_LATE_PAYMENT, why, "refund:late:" + p.getId());
        outbox.publish("PAYMENT_LATE", "PAYMENT", p.getId(), Json.obj("paymentId", p.getId(),
                "bookingId", b.getId(), "userId", b.getUser().getId(), "amount", p.getAmount()));
    }
}
