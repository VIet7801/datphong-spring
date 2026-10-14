package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    @NotNull(message = "Phòng cần đặt không được để trống")
    private Long roomId;

    private Long roomTypeId;

    @NotNull(message = "Ngày check-in không được để trống")
    @FutureOrPresent(message = "Ngày check-in không được ở quá khứ")
    private LocalDate checkinDate;

    @NotNull(message = "Ngày check-out không được để trống")
    private LocalDate checkoutDate;

    @NotNull(message = "Số lượng khách không được để trống")
    @Min(value = 1, message = "Phải có ít nhất 1 khách")
    private Integer guestsCount;

    /** Mã voucher áp dụng, có thể để trống nếu không dùng voucher. */
    private String voucherCode;

    @NotBlank(message = "idempotencyKey khong duoc de trong")
    private String idempotencyKey;

    private String quoteToken;

    private Long priceVersion;

    @Size(max = 500)
    private String specialRequest;

    @AssertTrue(message = "Ngày check-out phải sau ngày check-in")
    public boolean isValidDateRange() {
        if (checkinDate == null || checkoutDate == null) return true;
        return checkoutDate.isAfter(checkinDate);
    }
}
