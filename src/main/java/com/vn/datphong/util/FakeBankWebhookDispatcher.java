package com.vn.datphong.util;

import com.vn.datphong.dto.request.FakeBankWebhookRequest;
import com.vn.datphong.entity.FakeBankWebhookEvent;
import com.vn.datphong.repository.FakeBankWebhookEventRepository;
import com.vn.datphong.service.PaymentWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/** Gửi webhook ký HMAC tới module thanh toán (gọi trực tiếp trong tiến trình thay cho HTTP), retry có backoff khi bên nhận lỗi. */
@Component
public class FakeBankWebhookDispatcher {
    private static final Logger log = LoggerFactory.getLogger(FakeBankWebhookDispatcher.class);
    private static final int MAX_ATTEMPTS = 8;

    private final FakeBankWebhookEventRepository repository;
    private final PaymentWebhookService receiver;
    private final Clock clock;
    private final String secret;
    private final TransactionTemplate tx;

    public FakeBankWebhookDispatcher(FakeBankWebhookEventRepository repository, PaymentWebhookService receiver, Clock clock,
                                     @Value("${app.fakebank.webhook-secret}") String secret,
                                     PlatformTransactionManager txManager) {
        this.repository = repository;
        this.receiver = receiver;
        this.clock = clock;
        this.secret = secret;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelayString = "${app.fakebank.dispatch-ms:2000}")
    public void dispatchDue() {
        try {
            for (Long id : repository.findDueIds(LocalDateTime.now(clock), PageRequest.of(0, 50))) {
                sendOne(id);
            }
        } catch (RuntimeException e) {
            log.warn("Dispatch webhook lỗi: {}", e.toString());
        }
    }

    private void sendOne(Long id) {
        FakeBankWebhookEvent e = repository.findById(id).orElse(null);
        if (e == null || !"PENDING".equals(e.getStatus())) {
            return;
        }
        FakeBankWebhookRequest p = FakeBankWebhookRequest.signed(secret, e.getEventId(), e.getEventType(), e.getReference(),
                e.getIdempotencyKey(), e.getAmount(), e.getCurrency(), e.getFailureCode(), clock.instant().getEpochSecond());
        String error = null;
        try {
            receiver.receive(p);
        } catch (RuntimeException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        final String err = error;
        tx.executeWithoutResult(s -> {
            FakeBankWebhookEvent fresh = repository.findById(id).orElseThrow();
            fresh.setAttempts(fresh.getAttempts() + 1);
            if (err == null) {
                fresh.setStatus("SENT");
            } else if (fresh.getAttempts() >= MAX_ATTEMPTS) {
                fresh.setStatus("DEAD");
                fresh.setLastError(truncate(err));
            } else {
                long backoff = Math.min(300, 2L << fresh.getAttempts());
                fresh.setAvailableAt(LocalDateTime.now(clock).plusSeconds(backoff));
                fresh.setLastError(truncate(err));
            }
            repository.save(fresh);
        });
    }

    private String truncate(String s) {
        return s.length() > 290 ? s.substring(0, 290) : s;
    }
}
