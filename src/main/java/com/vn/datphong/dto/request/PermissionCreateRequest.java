package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionCreateRequest extends StrictRequest {
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "(?!ROLE_)[A-Z][A-Z0-9_]*",
            message = "Permission dùng mã viết hoa và không được bắt đầu bằng ROLE_")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
