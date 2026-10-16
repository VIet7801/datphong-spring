package com.vn.datphong.enums;

/**
 * Trạng thái tác vụ nhắc lịch nhận phòng.
 *
 * <p>Dùng tại: {@code ScheduledReminder.status}.</p>
 */
public enum ReminderStatus {
    /** Đã lên lịch, đang chờ đến thời điểm thực hiện. */
    SCHEDULED,

    /** Worker đã nhận quyền xử lý tác vụ. */
    CLAIMED,

    /** Đã đưa yêu cầu nhắc vào hàng đợi gửi. */
    QUEUED,

    /** Đã ghi nhận gửi lời nhắc thành công. */
    SENT,

    /** Tác vụ nhắc đã bị hủy. */
    CANCELLED,

    /** Tác vụ được bỏ qua theo điều kiện nghiệp vụ. */
    SKIPPED,

    /** Tác vụ nhắc đã thất bại. */
    FAILED
}
