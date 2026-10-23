package com.vn.datphong.service;

import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.RefundRequest;

import java.math.BigDecimal;
public interface RefundService {

    String KIND_CANCELLATION = "CANCELLATION";

    String KIND_LATE_PAYMENT = "LATE_PAYMENT";


    record Claim(Long refundId, String idempotencyKey, String debitReference, BigDecimal amount) {
    }


    RefundRequest request(Booking booking, Payment payment, BigDecimal amount, String kind, String reason, String key);


    /** Trả null khi chưa đến lượt, đang được xử lý hoặc đã kết thúc. force không bỏ qua lease đang giữ. */
    Claim claim(Long refundId, boolean force);


    void complete(Long refundId, FakeBankService.TxResult result);
}
