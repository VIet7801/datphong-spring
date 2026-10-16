package com.vn.datphong.enums;

/**
 * Tập trạng thái xác minh danh tính dự kiến của mô hình cũ.
 *
 * <p>Hiện chưa có thuộc tính entity nào sử dụng enum này.</p>
 *
 * <p>Hiện chưa được gắn với thuộc tính entity. KycDocument.status đang dùng DocReviewStatus.</p>
 */
public enum KycStatusType {
    /** Chưa có hồ sơ xác minh. */
    NONE,

    /** Đang chờ xác minh. */
    PENDING,

    /** Đã được chấp thuận xác minh. */
    APPROVED,

    /** Đã bị từ chối xác minh. */
    REJECTED
}
