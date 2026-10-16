package com.vn.datphong.enums;

/**
 * Trạng thái vòng đời đặt phòng và giá trị trước/sau trong lịch sử booking.
 *
 * <p>Dùng tại: {@code Booking.status}, {@code BookingStatusHistory.fromStatus}, {@code BookingStatusHistory.toStatus}.</p>
 *
 * <p>Enum chỉ định nghĩa tập mã, không tự thực hiện chuyển trạng thái, thu tiền hay giải phóng tồn phòng.</p>
 */
public enum BookingStatusType {
    /** Mã giữ chỗ cũ, được giữ để tương thích với luồng hiện có. */
    HOLD,

    /** Đang chờ thanh toán trong thời hạn giữ chỗ. */
    PENDING_PAYMENT,

    /** Đặt phòng đã được xác nhận. */
    CONFIRMED,

    /** Khách đã nhận phòng. */
    CHECKED_IN,

    /** Lượt lưu trú đã hoàn tất. */
    COMPLETED,

    /** Đặt phòng đã bị hủy. */
    CANCELLED,

    /** Đang xử lý yêu cầu hủy đặt phòng. */
    CANCELLATION_PENDING,

    /** Đang chờ xử lý hoàn tiền liên quan đến booking. */
    REFUND_PENDING,

    /** Mã đã hoàn tiền được giữ để tương thích với mô hình cũ. */
    REFUNDED,

    /** Đặt phòng đã hết hạn theo luồng giữ chỗ. */
    EXPIRED,

    /** Cần kiểm tra hoặc xử lý ngoại lệ. */
    NEEDS_ATTENTION,

    /** Đã xác nhận khách không đến nhận phòng. */
    NO_SHOW
}
