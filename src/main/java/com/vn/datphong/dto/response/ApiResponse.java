package com.vn.datphong.dto.response;

import com.vn.datphong.exception.ApiError;

/**
 * Vỏ bọc body trả về từ controller; ở luồng user, data chứa response DTO thay cho entity.
 * Đây vẫn là đối tượng Java; Spring MVC/Jackson chuyển nó thành JSON khi ghi HTTP response.
 * Trường success không tự quyết định HTTP status; filter dùng sendError không đi qua lớp này.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        String timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                "Success",
                data,
                null,
                java.time.Instant.now().toString()
        );
    }

    public static <T> ApiResponse<T> customsuccess(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                data,
                null,
                java.time.Instant.now().toString()
        );
    }

    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(
                false,
                message,
                null,
                error,
                java.time.Instant.now().toString()
        );
    }
}
