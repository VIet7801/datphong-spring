package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProfileUpdateRequest extends ProfileUpdateRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
