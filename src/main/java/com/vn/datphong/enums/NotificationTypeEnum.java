package com.vn.datphong.enums;

/**
 * Nhóm nội dung thông báo để phân loại khi hiển thị cho người nhận.
 *
 * <p>Dùng tại: {@code Notification.notifType}.</p>
 */
public enum NotificationTypeEnum {
    /** Thông báo liên quan đến đặt phòng. */
    BOOKING,

    /** Thông báo có tin nhắn. */
    MESSAGE,

    /** Thông báo nâng hạng thành viên. */
    RANK_UP,

    /** Thông báo từ hệ thống. */
    SYSTEM,

    /** Thông báo khuyến mãi. */
    PROMOTION
}
