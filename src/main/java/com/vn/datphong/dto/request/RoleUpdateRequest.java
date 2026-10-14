package com.vn.datphong.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {

    @Size(max = 100, message = "name không được quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "description không được quá 255 ký tự")
    private String description;
}
