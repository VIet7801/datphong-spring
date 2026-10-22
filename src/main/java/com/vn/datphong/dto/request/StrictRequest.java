package com.vn.datphong.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * Từ chối trường ngoài DTO, kể cả khi Jackson toàn cục bỏ qua trường lạ.
 * Ngăn gửi role/status/password vào API hồ sơ cá nhân.
 */
public abstract class StrictRequest {
    @JsonAnySetter
    public final void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Yêu cầu chứa trường không được phép");
    }
}
