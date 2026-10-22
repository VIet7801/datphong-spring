package com.vn.datphong.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
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
public class RoleUpdateRequest extends VersionedReasonRequest {

    @NotBlank
    @Size(max = 100, message = "name không được quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "description không được quá 255 ký tự")
    private String description;
}
