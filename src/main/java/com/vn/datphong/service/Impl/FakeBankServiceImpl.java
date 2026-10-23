package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.FakeBankAccount;
import com.vn.datphong.entity.FakeBankTransaction;
import com.vn.datphong.entity.FakeBankWebhookEvent;
import com.vn.datphong.repository.FakeBankAccountRepository;
import com.vn.datphong.repository.FakeBankTransactionRepository;
import com.vn.datphong.repository.FakeBankWebhookEventRepository;
import com.vn.datphong.service.FakeBankService;
import com.vn.datphong.util.FakeBankFaults;
import com.vn.datphong.util.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;


@Service
public class FakeBankServiceImpl implements FakeBankService {


    private final FakeBankAccountRepository accounts;
    private final FakeBankTransactionRepository transactions;
    private final FakeBankWebhookEventRepository webhooks;
    private final FakeBankFaults faults;
    private final Clock clock;
    private final int delaySeconds;

    public FakeBankServiceImpl(FakeBankAccountRepository accounts, FakeBankTransactionRepository transactions,
                               FakeBankWebhookEventRepository webhooks, FakeBankFaults faults, Clock clock,
                               @Value("${app.fakebank.delay-seconds:20}") int delaySeconds) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.webhooks = webhooks;
        this.faults = faults;
        this.clock = clock;
        this.delaySeconds = delaySeconds;
    }

    @Override
    @Transactional
    public TxResult debit(String idempotencyKey, String accountNo, BigDecimal amount, String currency, Scenario scenario) {
        Optional<FakeBankTransaction> prior = transactions.findByIdempotencyKey(idempotencyKey);
        if (prior.isPresent()) {
            return toResult(prior.get());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        FakeBankTransaction tx = new FakeBankTransaction();
        tx.setType("DEBIT");
        tx.setIdempotencyKey(idempotencyKey);
        tx.setAccountNo(accountNo == null ? "" : accountNo);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setReference("FB" + Hashing.randomCode(14));

        FakeBankAccount acc = accountNo == null ? null : accounts.findForUpdate(accountNo).orElse(null);
        String failure = null;
        if (acc == null) {
            failure = "ACCOUNT_NOT_FOUND";
        } else if (!acc.getCurrency().equals(currency)) {
            failure = "CURRENCY_MISMATCH";
        } else if (scenario == Scenario.DECLINED) {
            failure = "DECLINED";
        } else if (acc.getBalance().compareTo(amount) < 0) {
            failure = "INSUFFICIENT_FUNDS";
        }
        if (failure != null) {
            tx.setStatus("FAILED");
            tx.setFailureCode(failure);
            transactions.saveAndFlush(tx);
            return toResult(tx);
        }
        acc.setBalance(acc.getBalance().subtract(amount));
        tx.setStatus("SUCCESS");
        transactions.saveAndFlush(tx);

        switch (scenario) {
            case CALLBACK_LOST -> { /* tiền đã trừ nhưng webhook "thất lạc" */ }
            case DELAYED, TIMEOUT -> enqueue(tx, now.plusSeconds(delaySeconds), 1);
            case DUPLICATE -> enqueue(tx, now, 2);
            default -> enqueue(tx, now, 1);
        }
        return toResult(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TxResult> query(String idempotencyKey) {
        return transactions.findByIdempotencyKey(idempotencyKey).map(this::toResult);
    }

    /**
     * Hoàn tiền về tài khoản đã trả. Tổng hoàn không vượt số đã ghi nợ. Fault injection: lỗi (không đổi gì) hoặc
     * timeout SAU khi đã chuyển tiền -- ném ngoại lệ ở lớp gateway, giao dịch ở đây đã commit.
     */
    @Override
    @Transactional
    public TxResult refund(String idempotencyKey, String debitReference, BigDecimal amount) {
        Optional<FakeBankTransaction> prior = transactions.findByIdempotencyKey(idempotencyKey);
        if (prior.isPresent()) {
            return toResult(prior.get());
        }
        if (faults.consumeRefundFailure()) {
            return new TxResult("FAILED", null, "BANK_UNAVAILABLE", amount);
        }
        FakeBankTransaction original = transactions.findByReference(debitReference).orElse(null);
        FakeBankTransaction tx = new FakeBankTransaction();
        tx.setType("REFUND");
        tx.setIdempotencyKey(idempotencyKey);
        tx.setAmount(amount);
        tx.setReference("FR" + Hashing.randomCode(14));
        tx.setRelatedReference(debitReference);
        if (original == null || !"DEBIT".equals(original.getType()) || !"SUCCESS".equals(original.getStatus())) {
            tx.setAccountNo("");
            tx.setCurrency("VND");
            tx.setStatus("FAILED");
            tx.setFailureCode("ORIGINAL_NOT_FOUND");
            transactions.saveAndFlush(tx);
            return toResult(tx);
        }
        tx.setAccountNo(original.getAccountNo());
        tx.setCurrency(original.getCurrency());
        FakeBankAccount acc = accounts.findForUpdate(original.getAccountNo()).orElse(null);
        BigDecimal refunded = transactions.sumRefunded(debitReference);
        if (acc == null) {
            tx.setStatus("FAILED");
            tx.setFailureCode("ACCOUNT_NOT_FOUND");
        } else if (refunded.add(amount).compareTo(original.getAmount()) > 0) {
            tx.setStatus("FAILED");
            tx.setFailureCode("REFUND_EXCEEDS_CAPTURE");
        } else {
            acc.setBalance(acc.getBalance().add(amount));
            tx.setStatus("SUCCESS");
        }
        transactions.saveAndFlush(tx);
        return toResult(tx);
    }

    @Override
    public boolean shouldTimeoutAfterRefund() {
        return faults.consumeRefundTimeout();
    }

    private void enqueue(FakeBankTransaction tx, LocalDateTime at, int copies) {
        String eventId = "evt_" + Hashing.randomCode(16);
        for (int i = 0; i < copies; i++) {
            FakeBankWebhookEvent e = new FakeBankWebhookEvent();
            e.setEventId(eventId);
            e.setEventType("PAYMENT_SUCCEEDED");
            e.setReference(tx.getReference());
            e.setIdempotencyKey(tx.getIdempotencyKey());
            e.setAmount(tx.getAmount());
            e.setCurrency(tx.getCurrency());
            e.setAvailableAt(at);
            webhooks.save(e);
        }
    }

    private TxResult toResult(FakeBankTransaction t) {
        return new TxResult(t.getStatus(), t.getReference(), t.getFailureCode(), t.getAmount());
    }
}
