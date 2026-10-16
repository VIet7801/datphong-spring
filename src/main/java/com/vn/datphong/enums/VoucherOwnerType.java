package com.vn.datphong.enums;

/**
 * Loại chủ sở hữu voucher trong phần mở rộng khuyến mãi.
 *
 * <p>Dùng tại: {@code Voucher.ownerType}.</p>
 *
 * <p>ownerId cần được service kiểm tra theo ownerType, không phải một FK đa hình tự động.</p>
 */
public enum VoucherOwnerType {
    /** Voucher thuộc phạm vi hệ thống. */
    SYSTEM,

    /** Voucher thuộc chủ cơ sở lưu trú. */
    HOST,

    /** Voucher thuộc khách hàng. */
    CUSTOMER
}
