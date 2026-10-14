package com.vn.datphong.dto.request;

import com.vn.datphong.entity.Room;
import com.vn.datphong.entity.User;
import com.vn.datphong.entity.Voucher;
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
public class BookingRequest {
    private Long id;
    private String bookingCode;
    private User user;
    private Room room;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guestsCount = 1;
    private BookingStatusType status = BookingStatusType.HOLD;
    private LocalDateTime holdExpiresAt;
    private BigDecimal totalPrice;
    private String currency = "VND";
    private Voucher voucher;
    private BigDecimal loyaltyDiscountPercent = BigDecimal.ZERO;
}
