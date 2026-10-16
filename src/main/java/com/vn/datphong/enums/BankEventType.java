package com.vn.datphong.enums;

/**
 * Loại sự kiện thanh toán hoặc hoàn tiền nhận từ ngân hàng.
 *
 * <p>Dùng tại: {@code BankWebhookEvent.eventType}.</p>
 *
 * <p>Loại sự kiện khác với processingStatus, là trạng thái xử lý sự kiện trong hệ thống.</p>
 */
public enum BankEventType {
    /** Ngân hàng thông báo thanh toán thành công. */
    PAYMENT_SUCCEEDED,

    /** Ngân hàng thông báo thanh toán thất bại. */
    PAYMENT_FAILED,

    /** Ngân hàng thông báo hoàn tiền thành công. */
    REFUND_SUCCEEDED,

    /** Ngân hàng thông báo hoàn tiền thất bại. */
    REFUND_FAILED
}
