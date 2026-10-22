package com.vn.datphong.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có mã ổn định (ROOM_UNAVAILABLE, PRICE_CHANGED...) để client xử lý theo code, không theo message.
 * Là RuntimeException nên transaction Spring rollback mặc định; dùng noRollbackFor khi cần ghi nhận kết quả thất bại.
 */
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final transient Object details;

    public DomainException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public DomainException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    public static DomainException badRequest(String code, String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static DomainException unauthorized(String code, String message) {
        return new DomainException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN_SCOPE", message);
    }

    public static DomainException notFound(String code, String message) {
        return new DomainException(HttpStatus.NOT_FOUND, code, message);
    }

    public static DomainException conflict(String code, String message) {
        return new DomainException(HttpStatus.CONFLICT, code, message);
    }

    public static DomainException conflict(String code, String message, Object details) {
        return new DomainException(HttpStatus.CONFLICT, code, message, details);
    }
}
