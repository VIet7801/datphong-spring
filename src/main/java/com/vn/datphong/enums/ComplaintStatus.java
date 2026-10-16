package com.vn.datphong.enums;

/**
 * Trạng thái xử lý phản ánh gắn với hội thoại và booking.
 *
 * <p>Dùng tại: {@code ComplaintTicket.status}.</p>
 */
public enum ComplaintStatus {
    /** Phản ánh mới được mở. */
    OPEN,

    /** Đã tiếp nhận phản ánh. */
    ACKNOWLEDGED,

    /** Đang xử lý phản ánh. */
    IN_PROGRESS,

    /** Đã đưa ra kết quả giải quyết. */
    RESOLVED,

    /** Phản ánh đã được đóng. */
    CLOSED
}
