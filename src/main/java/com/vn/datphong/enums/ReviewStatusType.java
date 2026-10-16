package com.vn.datphong.enums;

/**
 * Trạng thái kiểm duyệt và hiển thị đánh giá của khách.
 *
 * <p>Dùng tại: {@code Review.status}.</p>
 */
public enum ReviewStatusType {
    /** Mã hiển thị cũ, giữ để tương thích, mã đích là PUBLISHED. */
    VISIBLE,

    /** Mã bị quản trị viên ẩn của luồng cũ, giữ để tương thích. */
    HIDDEN_BY_ADMIN,

    /** Đánh giá đang chờ kiểm duyệt. */
    PENDING_MODERATION,

    /** Đánh giá đã được công bố. */
    PUBLISHED,

    /** Đánh giá đã bị ẩn theo luồng đích. */
    HIDDEN
}
