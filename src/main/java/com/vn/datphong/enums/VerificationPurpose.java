package com.vn.datphong.enums;

/**
 * Mục đích sử dụng token xác minh trong luồng tài khoản lõi.
 *
 * <p>Dùng tại: {@code VerificationToken.purpose}.</p>
 *
 * <p>Token chỉ hợp lệ cho đúng mục đích, thời hạn và trạng thái sử dụng đã được service kiểm tra.</p>
 */
public enum VerificationPurpose {
    /** Xác minh địa chỉ email. */
    EMAIL_VERIFICATION,

    /** Đặt lại mật khẩu. */
    PASSWORD_RESET
}
