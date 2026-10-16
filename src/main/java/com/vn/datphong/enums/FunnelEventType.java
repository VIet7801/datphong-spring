package com.vn.datphong.enums;

/**
 * Mốc hành vi được ghi nhận để phân tích quá trình tìm và đặt phòng.
 *
 * <p>Dùng tại: {@code UserBehaviorLog.eventType}.</p>
 *
 * <p>Ghi nhận theo sự kiện thực tế, không suy diễn toàn bộ funnel chỉ từ trạng thái cuối của Booking.</p>
 */
public enum FunnelEventType {
    /** Đã hiển thị kết quả tìm kiếm. */
    SEARCH_SHOWN,

    /** Đã phát hành báo giá. */
    QUOTE_ISSUED,

    /** Đã tạo giữ chỗ. */
    HOLD_CREATED,

    /** Đã khởi tạo thanh toán. */
    PAYMENT_INITIATED,

    /** Đã ghi nhận thu tiền thành công. */
    PAYMENT_CAPTURED,

    /** Đã xác nhận đặt phòng. */
    BOOKING_CONFIRMED,

    /** Đã hoàn tất nhận phòng. */
    CHECKIN_COMPLETED
}
