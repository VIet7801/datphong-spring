package com.vn.datphong.enums;

/**
 * Vòng đời tệp media từ lúc đăng ký tải lên đến khi sử dụng hoặc xóa.
 *
 * <p>Dùng tại: {@code MediaAsset.status}.</p>
 */
public enum MediaAssetStatus {
    /** Đang chờ hoàn tất tải lên và xác nhận tệp. */
    PENDING,

    /** Tệp đã sẵn sàng được sử dụng. */
    READY,

    /** Đang xử lý xóa tệp. */
    DELETING,

    /** Yêu cầu tải lên chưa hoàn tất đã hết hạn. */
    PENDING_EXPIRED,

    /** Quá trình xử lý tệp thất bại. */
    FAILED,

    /** Tệp đã được ghi nhận là đã xóa. */
    DELETED
}
