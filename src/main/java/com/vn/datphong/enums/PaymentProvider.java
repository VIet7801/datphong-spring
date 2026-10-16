package com.vn.datphong.enums;

/**
 * Nhà cung cấp xử lý thanh toán, hoàn tiền và phát sự kiện ngân hàng.
 *
 * <p>Dùng tại: {@code BankWebhookEvent.provider}, {@code Payment.provider}, {@code Refund.provider}.</p>
 */
public enum PaymentProvider {
    /** Dịch vụ ngân hàng giả lập thuộc ranh giới tích hợp thanh toán của hệ thống. */
    FAKE_BANK
}
