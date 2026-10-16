package com.vn.datphong.enums;

/**
 * Trạng thái xử lý yêu cầu rút tiền trong phần mở rộng ví.
 *
 * <p>Dùng tại: {@code WithdrawalRequest.status}.</p>
 */
public enum WithdrawalStatusType {
    /** Yêu cầu rút tiền đang chờ xử lý. */
    PENDING,

    /** Yêu cầu rút tiền đã được duyệt. */
    APPROVED,

    /** Yêu cầu rút tiền đã bị từ chối. */
    REJECTED,

    /** Yêu cầu rút tiền đã hoàn tất. */
    COMPLETED
}
