package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.RefundRequest;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.enums.RefundDestinationType;
import com.vn.datphong.enums.RefundStatusType;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.repository.BookingStatusHistoryRepository;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.repository.RefundRequestRepository;
import com.vn.datphong.service.FakeBankService;
import com.vn.datphong.service.OutboxService;
import com.vn.datphong.service.RefundService;
import com.vn.datphong.util.CurrencyScale;
import com.vn.datphong.util.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/** Ba transaction ngắn; việc gọi ngân hàng nằm ở RefundProcessor, ngoài các transaction này. */
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    static final int MAX_ATTEMPTS = 5;
    static final int LEASE_SECONDS = 60;
    private static final Set<RefundStatusType> RETRYABLE = Set.of(RefundStatusType.REQUESTED,
            RefundStatusType.FAILED, RefundStatusType.UNKNOWN, RefundStatusType.TIMED_OUT,
            RefundStatusType.RECONCILING, RefundStatusType.PROCESSING);

    private final RefundRequestRepository refunds;
    private final BookingRepository bookings;
    private final BookingStatusHistoryRepository history;
    private final PaymentRepository payments;
    private final BookingLifecycle lifecycle;
    private final OutboxService outbox;
    private final Clock clock;

    @Override
    @Transactional
    public RefundRequest request(Booking booking, Payment payment, BigDecimal amount, String kind, String reason, String key) {
        if (booking == null || booking.getId() == null || payment == null || payment.getId() == null
                || amount == null || amount.signum() <= 0 || key == null || key.isBlank() || key.length() > 120
                || (!KIND_CANCELLATION.equals(kind) && !KIND_LATE_PAYMENT.equals(kind))) {
            throw DomainException.badRequest("VALIDATION_ERROR", "Yêu cầu hoàn tiền không hợp lệ");
        }
        Booking lockedBooking = bookings.findByIdForUpdate(booking.getId()).orElseThrow(this::notFound);
        Payment lockedPayment = payments.findByIdForUpdate(payment.getId()).orElseThrow(this::notFound);
        if (!lockedPayment.getBooking().getId().equals(lockedBooking.getId())) {
            throw DomainException.conflict("PAYMENT_BOOKING_MISMATCH", "Payment không thuộc booking");
        }
        try {
            amount = CurrencyScale.round(amount, lockedPayment.getCurrency(), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw DomainException.badRequest("VALIDATION_ERROR", "Số tiền hoàn không đúng đơn vị tiền tệ");
        }
        RefundRequest prior = refunds.findByIdempotencyKey(key).orElse(null);
        if (prior != null) {
            if (!Objects.equals(prior.getBooking().getId(), lockedBooking.getId()) || prior.getPayment() == null
                    || !Objects.equals(prior.getPayment().getId(), lockedPayment.getId())
                    || prior.getAmount().compareTo(amount) != 0 || !Objects.equals(prior.getKind(), kind)
                    || !Objects.equals(prior.getReason(), reason)) {
                throw DomainException.conflict("IDEMPOTENCY_CONFLICT", "Mã hoàn tiền đã được dùng cho yêu cầu khác");
            }
            return prior;
        }
        if (!balanced(lockedPayment)) {
            throw DomainException.conflict("REFUND_RECONCILIATION_REQUIRED", "Dữ liệu hoàn tiền cũ hoặc counter chưa khớp; cần đối soát trước");
        }
        if (!captured(lockedPayment)) {
            throw DomainException.conflict("PAYMENT_NOT_CAPTURED", "Payment chưa được xác nhận thu tiền");
        }
        BigDecimal committed = refunds.sumCommitted(lockedPayment.getId());
        if (committed.add(amount).compareTo(lockedPayment.getCapturedAmount()) > 0) {
            throw DomainException.conflict("REFUND_AMOUNT_EXCEEDED", "Tổng tiền hoàn vượt số tiền đã thu");
        }
        RefundRequest refund = refunds.saveAndFlush(RefundRequest.builder().booking(lockedBooking).payment(lockedPayment)
                .amount(amount).currency(lockedPayment.getCurrency()).kind(kind).reason(reason)
                .idempotencyKey(key).status(RefundStatusType.REQUESTED).build());
        publish("REFUND_REQUESTED", refund);
        return refund;
    }

    @Override
    @Transactional
    public Claim claim(Long refundId, boolean force) {
        RefundRequest refund = lock(refundId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!RETRYABLE.contains(refund.getStatus())
                && !(force && refund.getStatus() == RefundStatusType.NEEDS_ATTENTION)) return null;
        // Kể cả force cũng không được cướp một lượt đang chạy chưa hết lease.
        if (refund.getNextRetryAt() != null && refund.getNextRetryAt().isAfter(now)
                && (!force || refund.getStatus() == RefundStatusType.PROCESSING)) return null;
        if (!force && refund.getAttempts() >= MAX_ATTEMPTS) {
            attention(refund, "RETRY_EXHAUSTED", "Đã hết số lần tự động thử hoàn tiền");
            return null;
        }
        Payment payment = refund.getPayment();
        if (payment == null || !captured(payment) || !balanced(payment)
                || !payment.getBooking().getId().equals(refund.getBooking().getId())
                || refund.getRefundDestination() != RefundDestinationType.BANK
                || refund.getIdempotencyKey() == null || refund.getIdempotencyKey().isBlank()
                || refund.getAmount().signum() <= 0 || !Objects.equals(refund.getCurrency(), payment.getCurrency())
                || (!KIND_CANCELLATION.equals(refund.getKind()) && !KIND_LATE_PAYMENT.equals(refund.getKind()))
                || refunds.sumCommitted(payment.getId()).compareTo(payment.getCapturedAmount()) > 0
                || payment.getRefundedAmount().add(refund.getAmount()).compareTo(payment.getCapturedAmount()) > 0) {
            attention(refund, "REFUND_RECONCILIATION_REQUIRED", "Dữ liệu thu/hoàn tiền cần được đối soát");
            return null;
        }
        String reference = payment.getProviderReference();
        if (reference == null || reference.isBlank()) reference = payment.getGatewayTransactionRef();
        if (reference == null || reference.isBlank()) {
            attention(refund, "MISSING_DEBIT_REFERENCE", "Không có mã giao dịch đã thu tiền");
            return null;
        }
        refund.setStatus(RefundStatusType.PROCESSING);
        refund.setAttempts(refund.getAttempts() + 1);
        refund.setNextRetryAt(now.plusSeconds(LEASE_SECONDS));
        return new Claim(refundId, refund.getIdempotencyKey(), reference, refund.getAmount());
    }

    @Override
    @Transactional
    public void complete(Long refundId, FakeBankService.TxResult result) {
        RefundRequest refund = lock(refundId);
        if (refund.getStatus() == RefundStatusType.SUCCEEDED || refund.getStatus() == RefundStatusType.PAID
                || refund.getStatus() == RefundStatusType.REJECTED) return;
        if (refund.getAttempts() == 0) {
            throw DomainException.conflict("INVALID_STATE", "Yêu cầu hoàn tiền chưa được nhận xử lý");
        }
        if (result != null && !result.success() && refund.getStatus() != RefundStatusType.PROCESSING) return;
        if (result == null || result.amount() == null || result.amount().compareTo(refund.getAmount()) != 0
                || (result.success() && (result.reference() == null || result.reference().isBlank()))
                || (refund.getProviderReference() != null && result.reference() != null
                    && !refund.getProviderReference().equals(result.reference()))) {
            attention(refund, "BANK_RESULT_MISMATCH", "Kết quả ngân hàng không khớp yêu cầu hoàn tiền");
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (result.success()) {
            Payment payment = refund.getPayment();
            if (payment == null || !balanced(payment)
                    || payment.getRefundedAmount().add(refund.getAmount()).compareTo(payment.getCapturedAmount()) > 0) {
                attention(refund, "REFUND_RECONCILIATION_REQUIRED", "Ngân hàng báo thành công; cần đối soát số tiền nội bộ");
                return;
            }
            refund.setStatus(RefundStatusType.SUCCEEDED);
            refund.setProviderReference(result.reference());
            refund.setProcessedAt(now);
            refund.setNextRetryAt(null);
            refund.setFailureCode(null);
            refund.setFailureMessage(null);
            payment.setRefundedAmount(payment.getRefundedAmount().add(refund.getAmount()));
            finishBooking(refund);
            publish("REFUND_SUCCEEDED", refund);
            return;
        }
        // Timeout/không rõ kết quả vẫn giữ khoản cam kết; lần sau hỏi bank bằng đúng key cũ.
        boolean failed = "FAILED".equals(result.status());
        if (result.reference() != null) refund.setProviderReference(result.reference());
        refund.setFailureCode(result.failureCode() == null ? "BANK_RESULT_UNKNOWN" : result.failureCode());
        refund.setFailureMessage(failed ? "Ngân hàng chưa hoàn tiền" : "Chưa xác định được kết quả hoàn tiền");
        // Chỉ BANK_UNAVAILABLE là lỗi tạm thời chưa tạo giao dịch; lỗi nghiệp vụ cần kiểm tra.
        if (refund.getAttempts() >= MAX_ATTEMPTS || (failed && !"BANK_UNAVAILABLE".equals(result.failureCode()))) {
            attention(refund, refund.getFailureCode(), refund.getFailureMessage());
        } else {
            refund.setStatus(failed ? RefundStatusType.FAILED : RefundStatusType.UNKNOWN);
            refund.setNextRetryAt(now.plusSeconds(Math.min(300, 15L * refund.getAttempts())));
        }
        publish(failed ? "REFUND_FAILED" : "REFUND_UNKNOWN", refund);
    }

    /** Mọi đường ghi giữ cùng thứ tự khóa: booking -> payment -> refund, tránh deadlock với payment/cancel. */
    private RefundRequest lock(Long refundId) {
        Long bookingId = refunds.findBookingId(refundId).orElseThrow(this::notFound);
        bookings.findByIdForUpdate(bookingId).orElseThrow(this::notFound);
        refunds.findPaymentId(refundId).ifPresent(id -> payments.findByIdForUpdate(id).orElseThrow(this::notFound));
        return refunds.findByIdForUpdate(refundId).orElseThrow(this::notFound);
    }

    private boolean captured(Payment payment) {
        return payment.getCapturedAmount().signum() > 0 && (payment.getStatus() == PaymentStatusType.SUCCEEDED
                || payment.getStatus() == PaymentStatusType.SUCCESS || payment.getStatus() == PaymentStatusType.NEEDS_ATTENTION);
    }

    private boolean balanced(Payment payment) {
        return !refunds.hasCoreRefunds(payment.getId())
                && refunds.sumSucceeded(payment.getId()).compareTo(payment.getRefundedAmount()) == 0;
    }

    private void finishBooking(RefundRequest refund) {
        Booking booking = refund.getBooking();
        Payment payment = refund.getPayment();
        if (KIND_LATE_PAYMENT.equals(refund.getKind()) && payment.getStatus() == PaymentStatusType.NEEDS_ATTENTION
                && payment.getRefundedAmount().compareTo(payment.getCapturedAmount()) == 0) {
            payment.setStatus(PaymentStatusType.SUCCEEDED);
        }
        boolean outstanding = refunds.findByBooking(booking.getId()).stream()
                .anyMatch(r -> Objects.equals(r.getKind(), refund.getKind()) && r.getStatus() != RefundStatusType.SUCCEEDED);
        if (outstanding) return;
        if (KIND_CANCELLATION.equals(refund.getKind()) && booking.getStatus() == BookingStatusType.REFUND_PENDING) {
            lifecycle.transition(booking, BookingStatusType.REFUNDED, BookingStatusChangeSource.FAKE_BANK, null, "Đã hoàn tiền hủy booking");
        } else if (KIND_LATE_PAYMENT.equals(refund.getKind()) && booking.getStatus() == BookingStatusType.NEEDS_ATTENTION) {
            // Chỉ phục hồi EXPIRED khi lịch sử xác nhận đây là sự cố thanh toán đến muộn.
            boolean wasExpired = history.findFirstByBooking_IdOrderByIdDesc(booking.getId())
                    .filter(h -> h.getFromStatus() == BookingStatusType.EXPIRED
                            && h.getToStatus() == BookingStatusType.NEEDS_ATTENTION).isPresent();
            if (wasExpired) {
                lifecycle.transition(booking, BookingStatusType.EXPIRED, BookingStatusChangeSource.FAKE_BANK, null, "Đã hoàn tiền thanh toán đến muộn");
            }
        }
    }

    private void attention(RefundRequest refund, String code, String message) {
        refund.setStatus(RefundStatusType.NEEDS_ATTENTION);
        refund.setFailureCode(code);
        refund.setFailureMessage(message);
        refund.setNextRetryAt(null);
        publish("REFUND_NEEDS_ATTENTION", refund);
    }

    private void publish(String event, RefundRequest refund) {
        outbox.publish(event, "REFUND", refund.getId(), Json.obj("refundId", refund.getId(),
                "bookingId", refund.getBooking().getId(), "bookingCode", refund.getBooking().getBookingCode(),
                "userId", refund.getBooking().getUser().getId(), "amount", refund.getAmount(),
                "currency", refund.getCurrency(), "kind", refund.getKind(), "status", refund.getStatus().name(),
                "failureCode", refund.getFailureCode()));
    }

    private DomainException notFound() {
        return DomainException.notFound("REFUND_NOT_FOUND", "Không tìm thấy dữ liệu hoàn tiền");
    }
}
