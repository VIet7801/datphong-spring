package com.vn.datphong.dto.response;

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
public class HostBookingResponse {
    private Long id;

    private String bookingCode;

    private String status;

    private Long propertyId;

    private String propertyName;

    private Long roomId;

    private String roomName;

    private LocalDate checkinDate;

    private LocalDate checkoutDate;

    private Integer guests;

    private BigDecimal total;

    private String currency;

    private String guestName;

    private String guestEmail;

    private String guestPhone;

    private String specialRequest;

    private String hostNote;

    private Long version;

    private LocalDateTime actualCheckIn;

    private LocalDateTime actualCheckOut;
}
