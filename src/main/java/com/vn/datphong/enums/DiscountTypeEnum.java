package com.vn.datphong.enums;

/**
 * Cách diễn giải giá trị giảm giá của voucher trong phần mở rộng khuyến mãi.
 *
 * <p>Dùng tại: {@code Voucher.discountType}.</p>
 *
 * <p>Enum này không tự bật việc áp voucher vào công thức giá booking lõi.</p>
 */
public enum DiscountTypeEnum {
    /** Giảm theo tỷ lệ phần trăm. */
    PERCENT,

    /** Giảm một số tiền cố định. */
    FIXED_AMOUNT
}
