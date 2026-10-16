package com.vn.datphong.enums;

/**
 * Phân loại tác nhân thực hiện thao tác để ghi lịch sử và truy vết.
 *
 * <p>Dùng tại: {@code AuditLog.actorType}, {@code BookingStatusHistory.actorType}.</p>
 */
public enum ActorType {
    /** Người dùng thực hiện thao tác. */
    USER,

    /** Hệ thống tự thực hiện thao tác. */
    SYSTEM,

    /** Nhà cung cấp bên ngoài phát sinh thao tác hoặc sự kiện. */
    PROVIDER
}
