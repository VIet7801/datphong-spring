package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.PaymentRequest;
import com.vn.datphong.dto.response.PaymentResponse;
import com.vn.datphong.entity.Payment;
import com.vn.datphong.enums.PaymentStatusType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentMapper {

    public static Payment toEntity(PaymentRequest req) {
        return Payment.builder()
                .method(req.getMethod())
                .status(PaymentStatusType.PENDING)
                .build();
    }

    public static PaymentResponse toResponse(Payment e) {
        if(e==null) {return null;}
        return PaymentResponse.builder()
                .id(e.getId())
                .bookingId(e.getBooking()!=null?e.getBooking().getId():null)
                .method(e.getMethod())
                .amount(e.getAmount())
                .status(e.getStatus())
                .gatewayTransactionRef(e.getGatewayTransactionRef())
                .paidAt(e.getPaidAt())
                .paymentUrl(null)
                .build();
    }
}
