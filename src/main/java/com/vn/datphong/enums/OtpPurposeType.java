package com.vn.datphong.enums;

/**
 * Mục đích mã OTP trong mô hình xác minh cũ.
 *
 * <p>Dùng tại: {@code OtpVerification.purpose}.</p>
 *
 * <p>Giữ để tương thích OtpVerification. Luồng token lõi theo ADR dùng VerificationToken và VerificationPurpose, không tạo hai nguồn xác minh song song.</p>
 */
public enum OtpPurposeType {
    /** OTP phục vụ đăng ký tài khoản. */
    REGISTER,

    /** OTP phục vụ quên mật khẩu. */
    FORGOT_PASSWORD
}
