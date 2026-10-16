package com.vn.datphong.enums;

/**
 * Trạng thái xét duyệt và vận hành hồ sơ chủ cơ sở lưu trú.
 *
 * <p>Dùng tại: {@code HostProfile.hostStatus}.</p>
 */
public enum HostStatus {
    /** Hồ sơ chủ nhà đang chờ duyệt. */
    PENDING,

    /** Hồ sơ chủ nhà đã được duyệt. */
    APPROVED,

    /** Hồ sơ chủ nhà đã bị từ chối. */
    REJECTED,

    /** Hồ sơ chủ nhà đang bị đình chỉ. */
    SUSPENDED
}
