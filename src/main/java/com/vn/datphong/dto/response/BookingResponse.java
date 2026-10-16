package com.vn.datphong.dto.response;

import com.vn.datphong.enums.BookingStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;

    private Long userId;
    private String userFullName;

    private Long roomId;
    private String roomName;
    private String roomCode;

    private Long propertyId;
    private String propertyName;
    private Long roomTypeId;
    private String roomTypeName;
    private Long roomUnitId;
    private String roomUnitCode;

    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guestsCount;

    private BookingStatusType status;
    private Instant holdExpiresAt;

    private BigDecimal totalPrice;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private String currency;

    private String voucherCode;
    private BigDecimal loyaltyDiscountPercent;

    private String quoteToken;
    private Long priceVersion;
    private String specialRequest;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private BigDecimal cancellationFee;
    private BigDecimal refundableAmount;
    private Instant actualCheckIn;
    private Instant actualCheckOut;

    private Instant createdAt;
    private Instant updatedAt;
}
