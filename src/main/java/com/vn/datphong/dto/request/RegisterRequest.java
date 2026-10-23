package com.vn.datphong.dto.request;

import com.vn.datphong.util.EmailAddress;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class RegisterRequest extends StrictRequest {
    @NotBlank @Size(max = 150)
    private String fullName;
    @NotBlank @Email @Size(max = 150)
    private String email;
    @Pattern(regexp = "^0[0-9]{9}$")
    private String phone;
    @Size(min = 4, max = 50) @Pattern(regexp = "[a-zA-Z0-9._]+")
    private String username;
    @NotBlank @Size(min = 12, max = 72)
    private String password;
    @NotBlank @Size(max = 72)
    private String confirmPassword;
    @AssertTrue(message = "Cần đồng ý điều khoản sử dụng")
    private boolean termsAccepted;
    private Boolean gender;

    public void setEmail(String email) { this.email = EmailAddress.normalize(email); }

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatching() {
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }
}
