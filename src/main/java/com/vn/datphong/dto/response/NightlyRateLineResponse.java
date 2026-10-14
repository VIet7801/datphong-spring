package com.vn.datphong.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NightlyRateLineResponse {
    private LocalDate stayDate;
    private BigDecimal nightlyRate;
    private String currency;
    private Long priceVersion;
}
