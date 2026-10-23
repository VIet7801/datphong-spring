package com.vn.datphong.dto.request;

import com.vn.datphong.util.EmailAddress;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class LoginRequest extends StrictRequest {
    @Email @Size(max = 150)
    private String email;
    // Alias cho client cũ; API mới dùng email. Chỉ nhận một trong hai.
    @Size(max = 50)
    private String username;
    @NotBlank @Size(max = 512)
    private String password;

    public void setEmail(String email) { this.email = EmailAddress.normalize(email); }

    @AssertTrue(message = "Gửi email hoặc username, không gửi cả hai")
    public boolean isIdentifierValid() {
        return (email != null && !email.isBlank()) != (username != null && !username.isBlank());
    }
}
