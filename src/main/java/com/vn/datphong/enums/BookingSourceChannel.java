package com.vn.datphong.enums;

/**
 * Kênh phát sinh booking hoặc sự kiện hành vi, phục vụ phân tích nguồn truy cập.
 *
 * <p>Dùng tại: {@code Booking.sourceChannel}, {@code UserBehaviorLog.sourceChannel}.</p>
 */
public enum BookingSourceChannel {
    /** Phát sinh từ giao diện web. */
    WEB,

    /** Phát sinh từ ứng dụng di động. */
    MOBILE,

    /** Được nhân viên quản trị hỗ trợ thực hiện. */
    ADMIN_ASSISTED,

    /** Chưa xác định được kênh phát sinh. */
    UNKNOWN
}
