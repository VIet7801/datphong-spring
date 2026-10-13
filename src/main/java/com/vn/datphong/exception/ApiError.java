package com.vn.datphong.exception;
public record ApiError(
        String code,
        Object details
) {
}