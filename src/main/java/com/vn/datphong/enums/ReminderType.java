package com.vn.datphong.enums;

/**
 * Loại tác vụ nhắc lịch được hệ thống hỗ trợ.
 *
 * <p>Dùng tại: {@code ScheduledReminder.reminderType}.</p>
 *
 * <p>Mốc giờ và windowCode là dữ liệu cấu hình, không mã hóa cứng thành các giá trị enum khác nhau.</p>
 */
public enum ReminderType {
    /** Nhắc khách về lịch nhận phòng. */
    CHECKIN_REMINDER
}
