package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest extends StrictRequest {
    @NotNull
    @PositiveOrZero
    private Long version;

    @Size(max = 150)
    @Pattern(regexp = "(?s).*\\S.*", message = "Họ tên không được chỉ chứa khoảng trắng")
    private String fullName;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0")
    private String phone;

    private Boolean gender;

    @Size(max = 255)
    private String avatar;
}
