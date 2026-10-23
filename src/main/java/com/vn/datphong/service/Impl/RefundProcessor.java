package com.vn.datphong.service.Impl;

import com.vn.datphong.enums.RefundStatusType;
import com.vn.datphong.exception.BankTimeoutException;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.RefundRequestRepository;
import com.vn.datphong.service.FakeBankService;
import com.vn.datphong.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefundProcessor {
    private static final Logger log = LoggerFactory.getLogger(RefundProcessor.class);
    private static final Set<RefundStatusType> DUE = Set.of(RefundStatusType.REQUESTED, RefundStatusType.PROCESSING,
            RefundStatusType.FAILED, RefundStatusType.UNKNOWN, RefundStatusType.TIMED_OUT, RefundStatusType.RECONCILING);

    private final RefundRequestRepository repository;
    private final RefundService refunds;
    private final FakeBankGateway bank;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.refund.scan-ms:15000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void scan() {
        for (Long id : repository.findDueIds(DUE, LocalDateTime.now(clock), PageRequest.of(0, 50))) {
            try {
                process(id, false);
            } catch (RuntimeException e) {
                // Lease hết hạn thì lượt sau tiếp tục, kể cả bank đã commit nhưng cập nhật nội bộ bị lỗi.
                log.warn("Không xử lý được refund {}: {}", id, e.toString());
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(Long refundId, boolean force) {
        RefundService.Claim claim = refunds.claim(refundId, force);
        if (claim == null) return;
        FakeBankService.TxResult result;
        try {
            // Bank trả lại giao dịch cũ khi cùng key/cùng nội dung, kể cả lần trước bị timeout.
            result = bank.refund(claim.idempotencyKey(), claim.debitReference(), claim.amount());
        } catch (BankTimeoutException e) {
            result = new FakeBankService.TxResult("UNKNOWN", null, "BANK_TIMEOUT", claim.amount());
        } catch (DomainException e) {
            result = new FakeBankService.TxResult("FAILED", null, e.getCode(), claim.amount());
        }
        refunds.complete(refundId, result);
    }
}
