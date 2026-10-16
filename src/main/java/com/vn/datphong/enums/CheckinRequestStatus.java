package com.vn.datphong.enums;

/**
 * Trạng thái trao đổi và xử lý yêu cầu nhận phòng trong hội thoại.
 *
 * <p>Dùng tại: {@code CheckinRequest.status}.</p>
 *
 * <p>Chấp thuận yêu cầu hẹn giờ không tự đồng nghĩa booking đã CHECKED_IN.</p>
 */
public enum CheckinRequestStatus {
    /** Yêu cầu đang chờ phản hồi. */
    PENDING,

    /** Đã có đề xuất thời điểm nhận phòng khác. */
    COUNTER_PROPOSED,

    /** Yêu cầu đã được chấp thuận. */
    APPROVED,

    /** Yêu cầu đã bị từ chối. */
    REJECTED,

    /** Trao đổi yêu cầu đã được đóng. */
    CLOSED,

    /** Yêu cầu đã bị hủy. */
    CANCELLED
}
