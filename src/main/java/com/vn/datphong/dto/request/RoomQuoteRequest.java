package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomQuoteRequest {
    @NotNull
    private Long roomTypeId;

    @NotNull
    @FutureOrPresent
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotNull
    @Min(1)
    private Integer guestCount;

    @AssertTrue(message = "checkOut phai sau checkIn")
    public boolean isValidDateRange() {
        if (checkIn == null || checkOut == null) return true;
        return checkOut.isAfter(checkIn);
    }
}
