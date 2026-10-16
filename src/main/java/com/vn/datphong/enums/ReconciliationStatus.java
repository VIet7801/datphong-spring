package com.vn.datphong.enums;

/**
 * Trạng thái xử lý hồ sơ đối soát sai lệch thanh toán hoặc hoàn tiền.
 *
 * <p>Dùng tại: {@code ReconciliationCase.status}.</p>
 */
public enum ReconciliationStatus {
    /** Hồ sơ đối soát mới được mở. */
    OPEN,

    /** Đang thực hiện đối soát. */
    PROCESSING,

    /** Sai lệch đã được giải quyết. */
    RESOLVED,

    /** Cần người vận hành kiểm tra hoặc can thiệp. */
    NEEDS_ATTENTION
}
