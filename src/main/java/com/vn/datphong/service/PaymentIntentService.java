package com.vn.datphong.service;

import com.vn.datphong.dto.request.PaymentRequest;
import com.vn.datphong.dto.response.PaymentResponse;

import java.util.List;


public interface PaymentIntentService {

    PaymentResponse create(PaymentRequest req, String idempotencyKey);

    PaymentResponse get(Long id);

    List<PaymentResponse> forBooking(Long bookingId);
}
