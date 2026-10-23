package com.vn.datphong.enums;

/**
 * Phương thức thanh toán được ghi nhận trên giao dịch.
 *
 * <p>Dùng tại: {@code Payment.method}.</p>
 *
 * <p>Có giá trị enum không đồng nghĩa cổng thanh toán đó đã được tích hợp.</p>
 */
public enum PaymentMethodType {
    /** Mã phương thức VNPAY cũ, giữ để tương thích. */
    VNPAY,

    /** Mã phương thức MoMo cũ, giữ để tương thích. */
    MOMO,

    /** Mã phương thức ZaloPay cũ, giữ để tương thích. */
    ZALOPAY,

    /** Mã phương thức thẻ tín dụng cũ, giữ để tương thích. */
    CREDIT_CARD,

    /** Mã phương thức Apple Pay cũ, giữ để tương thích. */
    APPLE_PAY,

    /** Mã phương thức Google Pay cũ, giữ để tương thích. */
    GOOGLE_PAY,

    /** Thanh toán ghi nợ qua dịch vụ Fake Bank theo thiết kế lõi. */
    FAKE_BANK
}
