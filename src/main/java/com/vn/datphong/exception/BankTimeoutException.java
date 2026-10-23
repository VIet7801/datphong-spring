package com.vn.datphong.exception;

/** Bên gọi không nhận được phản hồi; bank CÓ THỂ đã xử lý. Phải đối soát theo idempotency key, không được giả định thất bại. */
public class BankTimeoutException extends RuntimeException {
    public BankTimeoutException(String message) {
        super(message);
    }
}
