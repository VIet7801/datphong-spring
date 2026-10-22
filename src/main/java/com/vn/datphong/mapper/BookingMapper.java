package com.vn.datphong.mapper;


import com.vn.datphong.dto.response.BookingHistoryResponse;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.dto.response.NightlyRateLineResponse;
import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.BookingNight;
import com.vn.datphong.entity.BookingStatusHistory;
import com.vn.datphong.util.CancellationPolicySnapshot;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookingMapper {
    public static BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())

                // Thông tin User
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .userFullName(booking.getUser() != null ? booking.getUser().getFullName() : null)

                // Thông tin Room
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomName(booking.getRoom() != null ? booking.getRoom().getName() : null)
                .roomCode(booking.getRoom() != null ? booking.getRoom().getCode() : null)

                // Thông tin Property & RoomType (Kiểm tra null an toàn nếu entity có liên kết)
                .propertyId(booking.getRoom() != null && booking.getRoom().getProperty() != null
                        ? booking.getRoom().getProperty().getId() : null)
                .propertyName(booking.getRoom() != null && booking.getRoom().getProperty() != null
                        ? booking.getRoom().getProperty().getName() : null)
                .roomTypeId(booking.getRoom() != null && booking.getRoom().getRoomTypeEntity() != null
                        ? booking.getRoom().getRoomTypeEntity().getId() : null)
                .roomTypeName(booking.getRoom() != null && booking.getRoom().getRoomTypeEntity() != null
                        ? booking.getRoom().getRoomTypeEntity().getName() : null)

                // Thời gian lưu trú & số lượng khách
                .checkinDate(booking.getCheckinDate())
                .checkoutDate(booking.getCheckoutDate())
                .guestsCount(booking.getGuestsCount())

                // Trạng thái & Giữ chỗ
                .status(booking.getStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())

                // Tài chính & Giá tiền
                .totalPrice(booking.getTotalPrice())
                .subtotalAmount(booking.getSubtotalAmount())
                .taxAmount(booking.getTaxAmount())
                .feeAmount(booking.getFeeAmount())
                .currency(booking.getCurrency())

                // Voucher & Khách hàng thân thiết
                .voucherCode(booking.getVoucher() != null ? booking.getVoucher().getCode() : null)
                .loyaltyDiscountPercent(booking.getLoyaltyDiscountPercent())

                // Các thông tin bổ sung & mốc thời gian sự kiện
                .quoteToken(booking.getQuoteToken())
                .priceVersion(booking.getPriceVersion())
                .specialRequest(booking.getSpecialRequest())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .cancellationFee(booking.getCancellationFee())
                .refundableAmount(booking.getRefundableAmount())
                .actualCheckIn(booking.getActualCheckIn())
                .actualCheckOut(booking.getActualCheckOut())

                // Thời gian khởi tạo / cập nhật (BaseEntity)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    /**
     * Dựng BookingResponse đầy đủ (kèm từng đêm và tên chính sách hủy). Phải gọi trong transaction vì
     * open-in-view đang tắt. {@code nights == null} nghĩa là không đính kèm danh sách đêm (dùng cho danh sách).
     */
    public static BookingResponse toResponse(Booking b, List<BookingNight> nights) {
        List<NightlyRateLineResponse> lines = new ArrayList<>();
        if (nights != null) {
            for (BookingNight n : nights) {
                lines.add(NightlyRateLineResponse.builder().stayDate(n.getStayDate()).nightlyRate(n.getNightlyRate())
                        .currency(n.getCurrency()).priceVersion(n.getPriceVersion()).build());
            }
        }
        String policy = b.getCancellationPolicySnapshot() == null ? null
                : CancellationPolicySnapshot.parse(b.getCancellationPolicySnapshot()).getName();
        return BookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .userId(b.getUser().getId())
                .userFullName(b.getUser().getFullName())
                .roomId(b.getRoom().getId())
                .roomName(b.getRoom().getName())
                .roomCode(b.getRoom().getCode())
                .propertyId(b.getProperty() == null ? null : b.getProperty().getId())
                .propertyName(b.getProperty() == null ? null : b.getProperty().getName())
                .roomTypeId(b.getRoomType() == null ? null : b.getRoomType().getId())
                .roomTypeName(b.getRoomType() == null ? null : b.getRoomType().getName())
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
                .nights(lines)
                .cancellationPolicyName(policy)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    public static BookingHistoryResponse toHistory(BookingStatusHistory h) {
        return BookingHistoryResponse.builder()
                .from(h.getFromStatus() == null ? null : h.getFromStatus().name())
                .to(h.getToStatus().name())
                .source(h.getSource().name())
                .actorId(h.getActor() == null ? null : h.getActor().getId())
                .reason(h.getReason())
                .at(h.getCreatedAt())
                .build();
    }
}
