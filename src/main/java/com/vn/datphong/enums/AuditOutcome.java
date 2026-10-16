package com.vn.datphong.enums;

/**
 * Phân loại kết quả thao tác được ghi vào nhật ký kiểm toán.
 *
 * <p>Dùng tại: {@code AuditLog.outcome}.</p>
 */
public enum AuditOutcome {
    /** Ghi nhận lần thử thực hiện thao tác. */
    ATTEMPT,

    /** Thao tác đã thành công. */
    SUCCESS,

    /** Thao tác đã thất bại. */
    FAILURE
}
