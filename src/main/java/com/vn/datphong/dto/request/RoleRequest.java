package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {
    @NotBlank(message = "Code không được để trống")
    @Size(max = 50, message = "Code không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 100, message = "Tên role không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "Description không được vượt quá 255 ký tự")
    private String description;
}
