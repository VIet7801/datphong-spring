package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.Payment;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.repository.PaymentRepository;
import com.vn.datphong.service.PaymentOutcomeService;
import com.vn.datphong.service.PaymentReconcileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class PaymentReconcileServiceImpl implements PaymentReconcileService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconcileService.class);
    private static final Set<PaymentStatusType> UNSETTLED = EnumSet.of(PaymentStatusType.INITIATED,
            PaymentStatusType.PROCESSING, PaymentStatusType.TIMED_OUT);

    private final PaymentRepository paymentRepository;
    private final FakeBankGateway bank;
    private final PaymentOutcomeService outcomes;
    private final Clock clock;
    private final int afterSeconds;

    public PaymentReconcileServiceImpl(PaymentRepository paymentRepository, FakeBankGateway bank, PaymentOutcomeService outcomes,
                                   Clock clock, @Value("${app.fakebank.reconcile-after-seconds:60}") int afterSeconds) {
        this.paymentRepository = paymentRepository;
        this.bank = bank;
        this.outcomes = outcomes;
        this.clock = clock;
        this.afterSeconds = afterSeconds;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.fakebank.reconcile-scan-ms:30000}")
    public void scan() {
        try {
            var ids = paymentRepository.findStaleIds(UNSETTLED, LocalDateTime.now(clock).minusSeconds(afterSeconds),
                    PageRequest.of(0, 50));
            for (Long id : ids) {
                try {
                    reconcile(id);
                } catch (RuntimeException e) {
                    log.warn("Đối soát payment {} lỗi: {}", id, e.toString());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Quét đối soát lỗi: {}", e.toString());
        }
    }

    /** @return trạng thái payment sau đối soát. */
    @Override
    public PaymentStatusType reconcile(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        if (!UNSETTLED.contains(p.getStatus())) {
            return p.getStatus();
        }
        var found = bank.queryByKey(p.getIdempotencyKey());
        if (found.isPresent()) {
            var r = found.get();
            outcomes.apply(paymentId, new PaymentOutcomeService.Outcome(r.success(), r.reference(), r.failureCode(), "RECONCILE"));
        } else {
            outcomes.apply(paymentId, new PaymentOutcomeService.Outcome(false, null, "BANK_NO_RECORD", "RECONCILE"));
        }
        return paymentRepository.findById(paymentId).orElseThrow().getStatus();
    }
}
