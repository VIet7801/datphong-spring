package com.vn.datphong.dto.request;

import com.vn.datphong.enums.OtpPurposeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @jakarta.validation.constraints.Size(min = 4, max = 10, message = "Mã OTP không hợp lệ")
    private String otpCode;

    @NotNull(message = "Mục đích xác thực OTP không được để trống")
    private OtpPurposeType purpose;
}
