package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class RefreshTokenRequest extends StrictRequest {
    @NotBlank @Size(max = 512)
    private String refreshToken;
}
