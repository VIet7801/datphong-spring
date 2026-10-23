package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.FakeBankWebhookRequest;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.service.PaymentWebhookService;
import com.vn.datphong.util.PaymentWebhookTx;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final long WINDOW_SECONDS = 300;

    private final PaymentWebhookTx tx;
    private final Clock clock;
    private final String secret;

    public PaymentWebhookServiceImpl(PaymentWebhookTx tx, Clock clock, @Value("${app.fakebank.webhook-secret}") String secret) {
        this.tx = tx;
        this.clock = clock;
        this.secret = secret;
    }

    @Override
    public String receive(FakeBankWebhookRequest p) {
        if (p == null || !p.validSignature(secret)) {
            throw DomainException.unauthorized("INVALID_SIGNATURE", "Chữ ký webhook không hợp lệ");
        }
        long skew = Math.abs(clock.instant().getEpochSecond() - p.getTimestamp());
        if (skew > WINDOW_SECONDS) {
            throw DomainException.badRequest("STALE_WEBHOOK", "Webhook ngoài cửa sổ thời gian cho phép");
        }
        try {
            return tx.process(p);
        } catch (DataIntegrityViolationException dup) {
            return "DUPLICATE";
        }
    }
}
