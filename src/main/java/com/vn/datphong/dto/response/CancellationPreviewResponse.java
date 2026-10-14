package com.vn.datphong.dto.response;

import com.vn.datphong.enums.BookingStatusType;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPreviewResponse {
    private Long bookingId;
    private BookingStatusType currentStatus;
    private BigDecimal paidAmount;
    private BigDecimal cancellationFee;
    private BigDecimal refundableAmount;
    private String currency;
    private String policySnapshot;
    private Boolean cancellable;
    private String reasonIfNotAllowed;
}
