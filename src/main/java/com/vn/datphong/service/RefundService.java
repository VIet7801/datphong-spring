package com.vn.datphong.service;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.RefundRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
public interface RefundService {

    public static final String KIND_CANCELLATION = "CANCELLATION";

    public static final String KIND_LATE_PAYMENT = "LATE_PAYMENT";


    public record Claim(Long refundId, String idempotencyKey, String debitReference, BigDecimal amount) {
    }


    RefundRequest request(Booking booking, Payment payment, BigDecimal amount, String kind, String reason, String key);


    Claim claim(Long refundId, boolean force);


    void complete(Long refundId, FakeBankService.TxResult result);
}