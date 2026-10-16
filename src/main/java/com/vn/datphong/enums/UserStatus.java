package com.vn.datphong.enums;

/**
 * Trạng thái tài khoản dùng cùng các điều kiện xác thực và khóa tài khoản.
 *
 * <p>Dùng tại: {@code User.status}.</p>
 *
 * <p>Đăng nhập còn kiểm tra User.active và quy tắc xác thực, không chỉ dựa vào enum này.</p>
 */
public enum UserStatus {
    /** Tài khoản đang ở trạng thái hoạt động. */
    ACTIVE,

    /** Tài khoản đang bị vô hiệu hóa. */
    IN_ACTIVE,

    /** Tài khoản đang bị khóa. */
    LOCKED,

    /** Tài khoản đang chờ xác minh email sau đăng ký. */
    PENDING_VERIFICATION
}
