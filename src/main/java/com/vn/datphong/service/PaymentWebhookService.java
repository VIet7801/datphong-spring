package com.vn.datphong.service;

import com.vn.datphong.dto.request.FakeBankWebhookRequest;


public interface PaymentWebhookService {

    String receive(FakeBankWebhookRequest p);
}
