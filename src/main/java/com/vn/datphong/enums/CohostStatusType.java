package com.vn.datphong.enums;

/**
 * Trạng thái quan hệ đồng chủ nhà trong phần mở rộng quản lý ủy quyền.
 *
 * <p>Dùng tại: {@code CoHost.status}.</p>
 *
 * <p>Quyền thao tác còn phụ thuộc phạm vi permissions và kiểm tra của service.</p>
 */
public enum CohostStatusType {
    /** Quan hệ đồng chủ nhà đang chờ chấp thuận. */
    PENDING,

    /** Quan hệ đồng chủ nhà đang có hiệu lực. */
    ACTIVE,

    /** Quyền đồng chủ nhà đã bị thu hồi. */
    REVOKED
}
