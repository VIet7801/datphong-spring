package com.vn.datphong.dto.request;

import com.vn.datphong.util.EmailAddress;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest extends StrictRequest {
    @NotBlank @Email @Size(max = 150)
    private String email;
    @NotBlank @Size(max = 512)
    private String token;

    public void setEmail(String email) { this.email = EmailAddress.normalize(email); }
}
