package com.vn.datphong.enums;

/**
 * Loại sự kiện nghiệp vụ dùng chọn mẫu email và phân loại yêu cầu gửi mail.
 *
 * <p>Dùng tại: {@code EmailTemplate.templateCode}, {@code MailEvent.eventType}.</p>
 */
public enum MailEventType {
    /** Thông báo tài khoản đã được tạo. */
    ACCOUNT_CREATED,

    /** Yêu cầu xác minh địa chỉ email. */
    EMAIL_VERIFICATION,

    /** Thông báo xác nhận đặt phòng. */
    BOOKING_CONFIRMED,

    /** Thông báo thanh toán thành công. */
    PAYMENT_SUCCEEDED,

    /** Thông báo thanh toán thất bại. */
    PAYMENT_FAILED,

    /** Thông báo hoàn tiền thành công. */
    REFUND_SUCCEEDED,

    /** Thông báo hoàn tiền thất bại. */
    REFUND_FAILED,

    /** Nhắc lịch nhận phòng. */
    CHECKIN_REMINDER,

    /** Thông báo phản hồi phản ánh. */
    COMPLAINT_REPLY,

    /** Thông báo xác nhận nhận phòng. */
    CHECKIN_CONFIRMED
}
