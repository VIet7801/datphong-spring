package com.vn.datphong.dto.request;

import com.vn.datphong.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Booking cần thanh toán không được để trống")
    private Long bookingId;

    /** Mặc định FAKE_BANK. */
    private PaymentMethodType method;

    /** Không bắt buộc ở body: khóa idempotency được gửi qua header Idempotency-Key. */
    private String idempotencyKey;

    /** URL FE muốn cổng thanh toán redirect về sau khi thanh toán xong. */
    private String returnUrl;

    @NotBlank(message = "Số tài khoản Fake Bank không được để trống")
    @Size(max = 40)
    private String accountNo;

    /** Kịch bản mô phỏng; chỉ có tác dụng khi app.fakebank.simulation-enabled=true. */
    @Size(max = 30)
    private String scenario;
}
