package com.vn.datphong.enums;

/**
 * Trạng thái xử lý một yêu cầu gửi email trong hàng đợi.
 *
 * <p>Dùng tại: {@code MailEvent.status}.</p>
 */
public enum MailStatus {
    /** Email đang chờ gửi. */
    QUEUED,

    /** Đang thực hiện gửi email. */
    SENDING,

    /** Đã ghi nhận gửi thành công, không có nghĩa người nhận đã đọc. */
    SENT,

    /** Đang chờ gửi lại. */
    RETRY,

    /** Chưa xác định được kết quả gửi. */
    UNKNOWN,

    /** Không vượt qua bước kiểm tra dữ liệu hoặc mẫu mail. */
    FAILED_VALIDATION,

    /** Yêu cầu gửi đã thất bại. */
    FAILED,

    /** Đã chuyển sang hàng đợi lỗi để kiểm tra riêng. */
    DLQ,

    /** Yêu cầu gửi đã bị hủy. */
    CANCELLED
}
