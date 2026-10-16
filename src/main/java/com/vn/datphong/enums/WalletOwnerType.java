package com.vn.datphong.enums;

/**
 * Loại chủ sở hữu ví trong phần mở rộng ví nội bộ.
 *
 * <p>Dùng tại: {@code Wallet.ownerType}.</p>
 *
 * <p>Ví nội bộ không thay thế tài khoản và sổ cái của dịch vụ Fake Bank.</p>
 */
public enum WalletOwnerType {
    /** Ví thuộc người dùng. */
    USER,

    /** Ví thuộc chủ cơ sở lưu trú. */
    HOST
}
