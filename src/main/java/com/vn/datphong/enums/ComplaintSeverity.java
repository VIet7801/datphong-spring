package com.vn.datphong.enums;

/**
 * Mức độ ưu tiên xử lý phản ánh của khách.
 *
 * <p>Dùng tại: {@code ComplaintTicket.severity}.</p>
 *
 * <p>Đây là tập mức độ được đề xuất trong catalog, không tự xác định thời hạn phản hồi.</p>
 */
public enum ComplaintSeverity {
    /** Mức độ thấp. */
    LOW,

    /** Mức độ thông thường. */
    NORMAL,

    /** Mức độ cao. */
    HIGH,

    /** Cần xử lý khẩn cấp. */
    URGENT
}
