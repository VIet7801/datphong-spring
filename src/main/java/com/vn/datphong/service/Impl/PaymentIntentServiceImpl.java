package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.PaymentRequest;
import com.vn.datphong.dto.response.PaymentResponse;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.exception.BankTimeoutException;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.service.CurrentUserService;
import com.vn.datphong.service.FakeBankService.TxResult;
import com.vn.datphong.service.PaymentIntentService;
import com.vn.datphong.service.PaymentOutcomeService;
import com.vn.datphong.service.PaymentTxService;
import com.vn.datphong.util.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PaymentIntentServiceImpl implements PaymentIntentService {

    private final PaymentTxService tx;
    private final PaymentRepository paymentRepository;
    private final PaymentOutcomeService outcomes;
    private final FakeBankGateway bank;
    private final CurrentUserService currentUser;

    @Override
    public PaymentResponse create(PaymentRequest req, String idempotencyKey) {
        User user = currentUser.require();
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw DomainException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "Thiếu hoặc sai header Idempotency-Key (tối đa 100 ký tự)");
        }
        String key = "pay:" + user.getId() + ":" + idempotencyKey;
        String hash = Hashing.requestHash(req.getBookingId(), req.getAccountNo(), req.getScenario() == null ? "" : req.getScenario());
        Payment p;
        try {
            p = tx.begin(user, req.getBookingId(), req.getAccountNo(), key, hash);
        } catch (DataIntegrityViolationException e) {
            // Hai request cùng key/booking đua nhau: một bên thắng, bên kia phát lại hoặc báo đang hoạt động.
            p = paymentRepository.findByIdempotencyKey(key).orElseThrow(() -> DomainException.conflict(
                    "PAYMENT_ALREADY_ACTIVE", "Booking đã có thanh toán đang hoạt động"));
        }
        if (p.getStatus() == PaymentStatusType.INITIATED) {
            callBank(p, req);
        }
        return view(paymentRepository.findById(p.getId()).orElseThrow());
    }

    private void callBank(Payment p, PaymentRequest req) {
        try {
            TxResult r = bank.debit(p.getIdempotencyKey(), req.getAccountNo(), p.getAmount(), p.getCurrency(), req.getScenario());
            if (r.success()) {
                tx.markProcessing(p.getId(), r.reference());
            } else {
                outcomes.apply(p.getId(), new PaymentOutcomeService.Outcome(false, r.reference(), r.failureCode(), "SYNC"));
            }
        } catch (BankTimeoutException timeout) {
            tx.markTimedOut(p.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        Payment p = paymentRepository.findById(id).orElseThrow(() -> DomainException.notFound("PAYMENT_NOT_FOUND", "Không tìm thấy payment"));
        authorize(p);
        return view(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> forBooking(Long bookingId) {
        List<Payment> list = paymentRepository.findByBooking(bookingId);
        List<PaymentResponse> out = new java.util.ArrayList<>();
        for (Payment p : list) {
            authorize(p);
            out.add(view(p));
        }
        return out;
    }

    void authorize(Payment p) {
        User u = currentUser.require();
        if (!p.getBooking().getUser().getId().equals(u.getId()) && !currentUser.isSupportOrAdmin()) {
            throw DomainException.notFound("PAYMENT_NOT_FOUND", "Không tìm thấy payment");
        }
    }

    PaymentResponse view(Payment p) {
        return PaymentResponse.builder().id(p.getId()).bookingId(p.getBooking().getId()).method(p.getMethod())
                .provider(p.getProvider()).amount(p.getAmount()).currency(p.getCurrency()).status(p.getStatus())
                .gatewayTransactionRef(p.getGatewayTransactionRef()).providerReference(p.getProviderReference())
                .failureCode(p.getFailureCode()).failureMessage(p.getFailureMessage()).paidAt(p.getPaidAt())
                .capturedAt(p.getCapturedAt()).createdAt(p.getCreatedAt()).refundedAmount(p.getRefundedAmount()).build();
    }
}
