package com.vn.datphong.enums;

/**
 * Trạng thái tiếp nhận và xử lý sự kiện ngân hàng trong inbox.
 *
 * <p>Dùng tại: {@code BankWebhookEvent.processingStatus}.</p>
 */
public enum InboxStatus {
    /** Đã tiếp nhận sự kiện. */
    RECEIVED,

    /** Worker đang xử lý sự kiện. */
    PROCESSING,

    /** Đã xử lý xong sự kiện. */
    PROCESSED,

    /** Sự kiện đang chờ xử lý lại. */
    RETRY,

    /** Sự kiện đã bị từ chối. */
    REJECTED,

    /** Sự kiện cần kiểm tra hoặc xử lý ngoại lệ. */
    NEEDS_ATTENTION
}
