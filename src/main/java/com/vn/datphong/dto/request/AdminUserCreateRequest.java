package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import com.vn.datphong.util.EmailAddress;

@Getter
@Setter
public class AdminUserCreateRequest extends StrictRequest {
    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    public void setEmail(String email) { this.email = EmailAddress.normalize(email); }

    @NotBlank
    @Size(min = 4, max = 50)
    @Pattern(regexp = "[a-zA-Z0-9._]+")
    private String username;

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;

    @Pattern(regexp = "^0[0-9]{9}$")
    private String phone;

    private Boolean gender;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
