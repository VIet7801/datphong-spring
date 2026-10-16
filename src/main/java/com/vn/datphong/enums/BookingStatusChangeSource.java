package com.vn.datphong.enums;

/**
 * Nguồn làm thay đổi trạng thái booking, phục vụ truy vết lịch sử.
 *
 * <p>Dùng tại: {@code BookingStatusHistory.source}.</p>
 */
public enum BookingStatusChangeSource {
    /** Thay đổi do khách đặt phòng. */
    CUSTOMER,

    /** Thay đổi do chủ cơ sở lưu trú. */
    HOST,

    /** Thay đổi do nhân viên hỗ trợ. */
    SUPPORT,

    /** Thay đổi do quản trị viên. */
    ADMIN,

    /** Thay đổi do xử lý nội bộ của hệ thống. */
    SYSTEM,

    /** Thay đổi có nguồn từ dịch vụ Fake Bank. */
    FAKE_BANK,

    /** Thay đổi do tác vụ chạy theo lịch. */
    SCHEDULER,

    /** Thay đổi do xử lý thông báo webhook. */
    WEBHOOK,

    /** Thay đổi do quá trình đối soát. */
    RECONCILIATION
}
