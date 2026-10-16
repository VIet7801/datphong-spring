package com.vn.datphong.dto.response;


import com.vn.datphong.enums.PaymentMethodType;
import com.vn.datphong.enums.PaymentProvider;
import com.vn.datphong.enums.PaymentStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private PaymentMethodType method;
    private PaymentProvider provider;
    private BigDecimal amount;
    private String currency;
    private PaymentStatusType status;
    private String gatewayTransactionRef;
    private String providerReference;
    private String failureCode;
    private String failureMessage;
    private Instant paidAt;
    private Instant capturedAt;
    //url để fe redirect sang vnpay/momo
    private String paymentUrl;
}
