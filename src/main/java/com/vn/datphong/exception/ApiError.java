package com.vn.datphong.exception;

import org.slf4j.MDC;

/** Phần lỗi trong ApiResponse: code nghiệp vụ, chi tiết (nếu có) và traceId để đối chiếu log. */
public record ApiError(
        String code,
        Object details,
        String traceId
) {
    public ApiError(String code, Object details) {
        this(code, details, MDC.get("traceId"));
    }
}
