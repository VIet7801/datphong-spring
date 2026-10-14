package com.vn.datphong.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomQuoteResponse {
    private Long propertyId;
    private Long roomTypeId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private Integer guestCount;
    private List<NightlyRateLineResponse> nightlyRates;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String quoteToken;
    private Long priceVersion;
    private LocalDateTime expiresAt;
    private String cancellationPolicyText;
}
