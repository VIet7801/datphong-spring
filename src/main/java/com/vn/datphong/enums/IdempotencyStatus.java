package com.vn.datphong.enums;

/**
 * Trạng thái xử lý một yêu cầu có khóa chống thực hiện trùng.
 *
 * <p>Dùng tại: {@code IdempotencyRecord.status}.</p>
 *
 * <p>Không mặc định lưu lỗi tạm thời thành kết quả vĩnh viễn. Việc kiểm tra key và payload do service thực hiện.</p>
 */
public enum IdempotencyStatus {
    /** Yêu cầu đang được xử lý. */
    IN_PROGRESS,

    /** Yêu cầu đã xử lý xong và có kết quả để phục vụ lần gọi lại. */
    COMPLETED
}
