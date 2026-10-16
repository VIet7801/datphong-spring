package com.vn.datphong.enums;

/**
 * Loại tình huống cần đối soát giữa hệ thống đặt phòng và ngân hàng.
 *
 * <p>Dùng tại: {@code ReconciliationCase.caseType}.</p>
 */
public enum ReconciliationType {
    /** Chưa xác định được kết quả thanh toán. */
    PAYMENT_UNKNOWN,

    /** Đã ghi nhận thu tiền nhưng booking chưa được xác nhận. */
    CAPTURE_WITHOUT_CONFIRMATION,

    /** Ghi nhận thu tiền muộn so với mốc xử lý thanh toán hoặc giữ chỗ. */
    LATE_CAPTURE,

    /** Chưa xác định được kết quả hoàn tiền. */
    REFUND_UNKNOWN,

    /** Số tiền giữa các nguồn không khớp. */
    AMOUNT_MISMATCH,

    /** Sự kiện ngân hàng chưa ghép được với giao dịch trong hệ thống. */
    UNMATCHED_EVENT
}
