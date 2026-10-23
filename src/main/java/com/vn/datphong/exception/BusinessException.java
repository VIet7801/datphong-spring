package com.vn.datphong.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;

    public BusinessException(int status, String message) {
        this(status, "BUSINESS_ERROR", message);
    }

    public BusinessException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }
}
