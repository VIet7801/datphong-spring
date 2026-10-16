package com.vn.datphong.enums;

/**
 * Chiều biến động số dư của giao dịch ví.
 *
 * <p>Dùng tại: {@code WalletTransaction.txType}.</p>
 */
public enum WalletTxType {
    /** Ghi tăng số dư ví. */
    CREDIT,

    /** Ghi giảm số dư ví. */
    DEBIT
}
