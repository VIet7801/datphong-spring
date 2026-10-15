package com.vn.datphong.mapper;


import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.entity.Booking;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookingMapper {
    public static BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .userId(b.getUser() != null ? b.getUser().getId() : null)
                .userFullName(b.getUser() != null ? b.getUser().getFullName() : null)
                .roomId(b.getRoom() != null ? b.getRoom().getId() : null)
                .roomName(b.getRoom() != null ? b.getRoom().getName() : null)
                .roomCode(b.getRoom() != null ? b.getRoom().getCode() : null)
                .propertyId(b.getProperty() != null ? b.getProperty().getId() : null)
                .propertyName(b.getProperty() != null ? b.getProperty().getName() : null)
                .roomTypeId(b.getRoomType() != null ? b.getRoomType().getId() : null)
                .roomTypeName(b.getRoomType() != null ? b.getRoomType().getName() : null)
                .roomUnitId(b.getRoomUnit() != null ? b.getRoomUnit().getId() : null)
                .roomUnitCode(b.getRoomUnit() != null ? b.getRoomUnit().getCode() : null)
                .checkinDate(b.getCheckinDate())
                .checkoutDate(b.getCheckoutDate())
                .guestsCount(b.getGuestsCount())
                .status(b.getStatus())
                .holdExpiresAt(b.getHoldExpiresAt())
                .totalPrice(b.getTotalPrice())
                .subtotalAmount(b.getSubtotalAmount())
                .taxAmount(b.getTaxAmount())
                .feeAmount(b.getFeeAmount())
                .currency(b.getCurrency())
                .voucherCode(b.getVoucher() != null ? b.getVoucher().getCode() : null)
                .loyaltyDiscountPercent(b.getLoyaltyDiscountPercent())
                .quoteToken(b.getQuoteToken())
                .priceVersion(b.getPriceVersion())
                .specialRequest(b.getSpecialRequest())
                .confirmedAt(b.getConfirmedAt())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .cancellationFee(b.getCancellationFee())
                .refundableAmount(b.getRefundableAmount())
                .actualCheckIn(b.getActualCheckIn())
                .actualCheckOut(b.getActualCheckOut())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
