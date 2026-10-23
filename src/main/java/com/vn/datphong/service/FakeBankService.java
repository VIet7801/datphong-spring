package com.vn.datphong.service;

import java.math.BigDecimal;
import java.util.Optional;


public interface FakeBankService {

    public enum Scenario { SUCCESS, DECLINED, TIMEOUT, CALLBACK_LOST, DUPLICATE, DELAYED }


    public record TxResult(String status, String reference, String failureCode, BigDecimal amount) {
        public boolean success() {
            return "SUCCESS".equals(status);
        }
    }

    TxResult debit(String idempotencyKey, String accountNo, BigDecimal amount, String currency, Scenario scenario);

    Optional<TxResult> query(String idempotencyKey);


    TxResult refund(String idempotencyKey, String debitReference, BigDecimal amount);

    boolean shouldTimeoutAfterRefund();
}
