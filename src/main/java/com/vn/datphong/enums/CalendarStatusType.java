package com.vn.datphong.enums;

/**
 * Trạng thái hiển thị của phòng theo ngày trong lịch phòng cũ.
 *
 * <p>Dùng tại: {@code RoomCalendar.status}.</p>
 *
 * <p>RoomCalendar là dữ liệu lịch/projection tương thích. Không dùng thay RoomAllocation và RoomInventoryBlock làm nguồn kiểm tra tồn phòng.</p>
 */
public enum CalendarStatusType {
    /** Ngày được hiển thị là còn phòng. */
    AVAILABLE,

    /** Ngày được hiển thị là đã có đặt phòng. */
    BOOKED,

    /** Ngày được hiển thị là bị khóa. */
    BLOCKED
}
