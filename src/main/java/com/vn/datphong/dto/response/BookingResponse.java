package com.vn.datphong.dto.response;

import com.vn.datphong.enums.BookingStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime holdExpiresAt;

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
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private BigDecimal cancellationFee;
    private BigDecimal refundableAmount;
    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
