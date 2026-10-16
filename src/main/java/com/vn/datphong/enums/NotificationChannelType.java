package com.vn.datphong.enums;

/**
 * Kênh gửi thông báo trong mô hình thông báo hiện có.
 *
 * <p>Dùng tại: {@code Notification.channel}.</p>
 */
public enum NotificationChannelType {
    /** Thông báo trong ứng dụng. */
    IN_APP,

    /** Thông báo qua email. */
    EMAIL,

    /** Thông báo qua cả ứng dụng và email. */
    BOTH
}
