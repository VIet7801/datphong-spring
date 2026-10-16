package com.vn.datphong.enums;

/**
 * Trạng thái xử lý một lần thanh toán, độc lập với trạng thái booking và refund.
 *
 * <p>Dùng tại: {@code Payment.status}.</p>
 *
 * <p>Luồng lõi giữ Payment ở SUCCEEDED sau khi hoàn tiền, theo dõi số tiền hoàn bằng Refund riêng.</p>
 */
public enum PaymentStatusType {
    /** Mã chờ xử lý của luồng cũ, giữ để tương thích. */
    PENDING,

    /** Đã khởi tạo yêu cầu thanh toán. */
    INITIATED,

    /** Thanh toán đang được xử lý. */
    PROCESSING,

    /** Mã thành công cũ, giữ để tương thích, mã đích là SUCCEEDED. */
    SUCCESS,

    /** Thanh toán đã thành công theo luồng đích. */
    SUCCEEDED,

    /** Thanh toán đã thất bại. */
    FAILED,

    /** Quá thời gian chờ xử lý, cần xác minh theo quy tắc nghiệp vụ. */
    TIMED_OUT,

    /** Mã cũ được giữ để tương thích, không dùng cho luồng hoàn tiền lõi mới. */
    REFUNDED,

    /** Thanh toán cần kiểm tra hoặc xử lý ngoại lệ. */
    NEEDS_ATTENTION,

    /** Chưa xác định được kết quả thanh toán. */
    UNKNOWN,

    /** Đang đối soát kết quả thanh toán. */
    RECONCILING
}
