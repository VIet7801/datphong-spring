package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRateUpsertRequest {
    @NotNull
    private Long roomTypeId;

    @NotNull
    private LocalDate stayDate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal nightlyRate;

    @Size(max = 5)
    private String currency;

    private Boolean closedToSale;
}
