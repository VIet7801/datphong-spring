package com.vn.datphong.enums;

/**
 * Trạng thái hoàn tiền, dùng chung cho Refund lõi và RefundRequest tương thích.
 *
 * <p>Dùng tại: {@code Refund.status}, {@code RefundRequest.status}.</p>
 *
 * <p>Không tự đồng nhất các mã cũ với mã đích hoặc giải phóng khoản giữ hoàn tiền chỉ từ tên trạng thái.</p>
 */
public enum RefundStatusType {
    /** Mã chờ xử lý của RefundRequest cũ. */
    PENDING,

    /** Đã có yêu cầu thực thi hoàn tiền, không mặc định là chờ phê duyệt riêng. */
    REQUESTED,

    /** Mã đã phê duyệt của luồng RefundRequest cũ. */
    APPROVED,

    /** Đang thực hiện hoàn tiền. */
    PROCESSING,

    /** Mã bị từ chối của luồng RefundRequest cũ. */
    REJECTED,

    /** Hoàn tiền đã thất bại. */
    FAILED,

    /** Mã quá hạn được giữ để tương thích, không đủ để tự kết luận ngân hàng chưa hoàn tiền. */
    TIMED_OUT,

    /** Mã đã chi tiền của luồng cũ, giữ để tương thích. */
    PAID,

    /** Hoàn tiền đã thành công theo luồng lõi. */
    SUCCEEDED,

    /** Cần kiểm tra hoặc xử lý ngoại lệ hoàn tiền. */
    NEEDS_ATTENTION,

    /** Chưa xác định được kết quả hoàn tiền. */
    UNKNOWN,

    /** Đang đối soát kết quả hoàn tiền. */
    RECONCILING
}
