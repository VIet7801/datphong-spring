package com.vn.datphong.enums;

/**
 * Trạng thái đăng ký gói VIP của chủ cơ sở lưu trú trong phần mở rộng.
 *
 * <p>Dùng tại: {@code HostVipSubscription.status}.</p>
 */
public enum VipSubStatusType {
    /** Gói đăng ký đang ở trạng thái hoạt động. */
    ACTIVE,

    /** Gói đăng ký đã hết hạn. */
    EXPIRED,

    /** Gói đăng ký đã bị hủy. */
    CANCELLED
}
