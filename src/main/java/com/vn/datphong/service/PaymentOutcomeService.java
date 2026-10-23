package com.vn.datphong.service;

/**
 * Điểm DUY NHẤT áp kết quả thanh toán lên payment + booking, dùng chung cho webhook, đối soát và phản hồi đồng bộ.
 * Idempotent: áp lại cùng kết quả là no-op. Khóa theo thứ tự booking -> payment.
 */
public interface PaymentOutcomeService {


    public record Outcome(boolean success, String reference, String failureCode, String source) {
    }

    void apply(Long paymentId, Outcome o);
}
