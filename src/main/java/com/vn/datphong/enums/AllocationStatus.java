package com.vn.datphong.enums;

/**
 * Trạng thái giữ chỗ của một phòng trong khoảng ngày lưu trú.
 *
 * <p>Dùng tại: {@code RoomAllocation.status}.</p>
 *
 * <p>Khách đã check-in vẫn có allocation CONFIRMED. Trạng thái này không thay thế Booking.status.</p>
 */
public enum AllocationStatus {
    /** Đang giữ chỗ tạm thời cho booking. */
    HELD,

    /** Đã xác nhận phân bổ phòng cho booking. */
    CONFIRMED,

    /** Đã giải phóng phần giữ chỗ. */
    RELEASED
}
