package com.vn.datphong.service.Impl;


import com.vn.datphong.exception.BankTimeoutException;
import com.vn.datphong.service.FakeBankService;
import com.vn.datphong.util.FakeBankWebhookDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class FakeBankGateway {
    private final FakeBankService bank;
    private final FakeBankWebhookDispatcher dispatcher;
    private final boolean simulationEnabled;

    public FakeBankGateway(FakeBankService bank, FakeBankWebhookDispatcher dispatcher,
                           @Value("${app.fakebank.simulation-enabled:true}") boolean simulationEnabled) {
        this.bank = bank;
        this.dispatcher = dispatcher;
        this.simulationEnabled = simulationEnabled;
    }

    public FakeBankService.TxResult debit(String idempotencyKey, String accountNo, BigDecimal amount, String currency, String scenarioName) {
        FakeBankService.Scenario sc = resolve(scenarioName);
        FakeBankService.TxResult r;
        try {
            r = bank.debit(idempotencyKey, accountNo, amount, currency, sc);
        } catch (DataIntegrityViolationException dup) {
            r = bank.query(idempotencyKey).orElseThrow(() -> dup);
        }
        if (sc == FakeBankService.Scenario.TIMEOUT && r.success()) {
            throw new BankTimeoutException("Bank không phản hồi kịp (mô phỏng)");
        }
        dispatcher.dispatchDue();
        return r;
    }

    public Optional<FakeBankService.TxResult> queryByKey(String idempotencyKey) {
        return bank.query(idempotencyKey);
    }

    public FakeBankService.TxResult refund(String idempotencyKey, String debitReference, BigDecimal amount) {
        FakeBankService.TxResult r;
        try {
            r = bank.refund(idempotencyKey, debitReference, amount);
        } catch (DataIntegrityViolationException dup) {
            r = bank.query(idempotencyKey).orElseThrow(() -> dup);
        }
        if (r.success() && bank.shouldTimeoutAfterRefund()) {
            throw new BankTimeoutException("Bank không phản hồi kịp sau khi hoàn tiền (mô phỏng)");
        }
        return r;
    }

    private FakeBankService.Scenario resolve(String name) {
        if (!simulationEnabled || name == null || name.isBlank()) {
            return FakeBankService.Scenario.SUCCESS;
        }
        try {
            return FakeBankService.Scenario.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FakeBankService.Scenario.SUCCESS;
        }
    }
}
