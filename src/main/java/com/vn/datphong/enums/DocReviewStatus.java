package com.vn.datphong.enums;

/**
 * Kết quả xét duyệt một giấy tờ xác minh danh tính.
 *
 * <p>Dùng tại: {@code KycDocument.status}.</p>
 */
public enum DocReviewStatus {
    /** Giấy tờ đang chờ xét duyệt. */
    PENDING,

    /** Giấy tờ đã được chấp thuận. */
    APPROVED,

    /** Giấy tờ đã bị từ chối. */
    REJECTED
}
