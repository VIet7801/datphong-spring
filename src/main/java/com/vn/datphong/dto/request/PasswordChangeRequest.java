package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest extends StrictRequest {
    @NotNull
    @PositiveOrZero
    private Long version;

    @NotBlank
    @Size(max = 512)
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 72)
    private String newPassword;
}
