package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreateRequest extends StrictRequest {

    @NotBlank(message = "code không được để trống")
    @Size(max = 50, message = "code không được quá 50 ký tự")
    @jakarta.validation.constraints.Pattern(
            regexp = "^ROLE_[A-Z][A-Z0-9_]*$",
            message = "code có dạng ROLE_SUPPORT, ROLE_CONTENT_MODERATOR")
    private String code;

    @NotBlank(message = "name không được để trống")
    @Size(max = 100, message = "name không được quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "description không được quá 255 ký tự")
    private String description;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
