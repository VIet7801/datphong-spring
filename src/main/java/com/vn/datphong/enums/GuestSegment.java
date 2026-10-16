package com.vn.datphong.enums;

/**
 * Phân nhóm khách được chụp lại tại thời điểm tạo booking để phục vụ báo cáo.
 *
 * <p>Dùng tại: {@code Booking.guestSegmentSnapshot}.</p>
 *
 * <p>Giữ nguyên snapshot lịch sử, không tính lại theo trạng thái hiện tại của tài khoản.</p>
 */
public enum GuestSegment {
    /** Chưa có booking COMPLETED trước thời điểm tạo booking này. */
    NEW,

    /** Đã có booking COMPLETED trước thời điểm tạo booking này. */
    RETURNING,

    /** Không đủ dữ liệu để xác định nhóm khách. */
    UNKNOWN
}
