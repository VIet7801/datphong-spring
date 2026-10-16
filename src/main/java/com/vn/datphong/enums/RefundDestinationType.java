package com.vn.datphong.enums;

/**
 * Nơi nhận tiền hoàn trong mô hình RefundRequest mở rộng.
 *
 * <p>Dùng tại: {@code RefundRequest.refundDestination}.</p>
 *
 * <p>Đây là phân loại của luồng cũ/mở rộng, không tự bật hoàn về ví trong luồng Refund lõi.</p>
 */
public enum RefundDestinationType {
    /** Hoàn tiền về ví. */
    WALLET,

    /** Hoàn tiền về ngân hàng. */
    BANK
}
