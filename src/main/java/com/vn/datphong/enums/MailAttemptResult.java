package com.vn.datphong.enums;

/**
 * Kết quả của một lần thử gửi email, tách biệt với trạng thái toàn bộ sự kiện mail.
 *
 * <p>Dùng tại: {@code MailDeliveryAttempt.result}.</p>
 */
public enum MailAttemptResult {
    /** Đã bắt đầu lần thử gửi. */
    STARTED,

    /** Lần thử gửi được ghi nhận thành công. */
    SUCCEEDED,

    /** Lỗi tạm thời có thể xem xét thử lại. */
    TEMPORARY_FAILURE,

    /** Lỗi được phân loại là không thể khắc phục bằng cách gửi lại như cũ. */
    PERMANENT_FAILURE,

    /** Chưa xác định được kết quả lần gửi. */
    UNKNOWN
}
