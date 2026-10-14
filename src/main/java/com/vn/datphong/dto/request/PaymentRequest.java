package com.vn.datphong.dto.request;

import com.vivu.booking.enums.PaymentMethodType;
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
public class PaymentRequest {

    @NotNull(message = "Booking cần thanh toán không được để trống")
    private Long bookingId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethodType method;

    /** URL FE muốn cổng thanh toán redirect về sau khi thanh toán xong. */
    @NotBlank(message = "returnUrl không được để trống")
    private String returnUrl;
}
