package com.vn.datphong.util;

import com.vn.datphong.dto.request.FakeBankWebhookRequest;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.PaymentWebhookEvent;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.repository.PaymentWebhookEventRepository;
import com.vn.datphong.service.PaymentOutcomeService;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookTx {
    private final PaymentWebhookEventRepository events;
    private final PaymentRepository payments;
    private final PaymentOutcomeService outcomes;

    public PaymentWebhookTx(PaymentWebhookEventRepository events, PaymentRepository payments, PaymentOutcomeService outcomes) {
        this.events = events;
        this.payments = payments;
        this.outcomes = outcomes;
    }

    @org.springframework.transaction.annotation.Transactional
    public String process(FakeBankWebhookRequest p) {
        PaymentWebhookEvent e = new PaymentWebhookEvent();
        e.setEventId(p.getEventId());
        e.setEventType(p.getType());
        events.saveAndFlush(e); // vi phạm unique => event đã xử lý
        Payment pay = payments.findByIdempotencyKey(p.getIdempotencyKey())
                .orElseThrow(() -> DomainException.notFound("PAYMENT_NOT_FOUND", "Không tìm thấy payment cho webhook"));
        if (pay.getAmount().compareTo(p.getAmount()) != 0 || !pay.getCurrency().equals(p.getCurrency())) {
            throw DomainException.badRequest("AMOUNT_MISMATCH", "Số tiền/tiền tệ webhook không khớp payment");
        }
        e.setPaymentId(pay.getId());
        boolean ok = FakeBankWebhookRequest.PAYMENT_SUCCEEDED.equals(p.getType());
        outcomes.apply(pay.getId(), new PaymentOutcomeService.Outcome(ok, p.getReference(), p.getFailureCode(), "WEBHOOK"));
        return "OK";
    }
}
