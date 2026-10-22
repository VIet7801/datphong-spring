package com.vn.datphong.service.Impl;


import com.vn.datphong.dto.response.BookingHistoryResponse;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.dto.response.NightlyRateLineResponse;
import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.BookingNight;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.repository.BookingNightRepository;
import com.vn.datphong.repository.BookingStatusHistoryRepository;
import com.vn.datphong.util.CancellationPolicySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@RequiredArgsConstructor
public class BookingViewServiceImpl {
    private final BookingNightRepository nightRepository;
    private final BookingStatusHistoryRepository historyRepository;

    private final com.vn.datphong.repository.BookingRepository bookingRepository;


    /** Nạp và dựng view trong một transaction (open-in-view đang tắt nên không được map entity đã detach). */
    @Transactional(readOnly = true)
    public BookingResponse viewById(Long id, boolean withNights) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("BOOKING_NOT_FOUND: Không tìm thấy booking"));
        return view(b, withNights);
    }

    @Transactional(readOnly = true)
    public BookingResponse view(Booking b, boolean withNights) {
        List<NightlyRateLineResponse> nights = new ArrayList<>();
        if (withNights) {
            for (BookingNight n : nightRepository.findByBooking(b.getId())) {
                nights.add(NightlyRateLineResponse.builder().stayDate(n.getStayDate()).nightlyRate(n.getNightlyRate())
                        .currency(n.getCurrency()).priceVersion(n.getPriceVersion()).build());
            }
        }
        String policy = null;
        if (b.getCancellationPolicySnapshot() != null) {
            policy = CancellationPolicySnapshot.parse(b.getCancellationPolicySnapshot()).getName();
        }
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
                .nights(nights)
                .cancellationPolicyName(policy)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> history(Long bookingId) {
        List<BookingHistoryResponse> out = new ArrayList<>();
        historyRepository.findByBooking(bookingId).forEach(h -> out.add(new BookingHistoryResponse(
                h.getFromStatus() == null ? null : h.getFromStatus().name(), h.getToStatus().name(),
                h.getSource().name(), h.getActor() == null ? null : h.getActor().getId(), h.getReason(), h.getCreatedAt())));
        return out;
    }
}


