package com.vn.datphong.enums;

/**
 * Trạng thái xử lý sự kiện outbox được ghi cùng giao dịch nghiệp vụ.
 *
 * <p>Dùng tại: {@code OutboxEvent.status}.</p>
 */
public enum OutboxStatus {
    /** Sự kiện đang chờ xử lý. */
    PENDING,

    /** Worker đang xử lý sự kiện. */
    PROCESSING,

    /** Mã đã gửi của luồng cũ, giữ để tương thích. */
    SENT,

    /** Mã thất bại của luồng cũ, giữ để tương thích. */
    FAILED,

    /** Sự kiện đã xử lý xong theo luồng đích. */
    PROCESSED,

    /** Sự kiện đang chờ thử lại. */
    RETRY,

    /** Sự kiện đã chuyển sang hàng đợi lỗi để xử lý riêng. */
    DLQ
}
