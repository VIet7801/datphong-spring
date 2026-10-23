package com.vn.datphong.service;

import com.vn.datphong.enums.PaymentStatusType;

public interface PaymentReconcileService {

    void scan();


    /** @return trạng thái payment sau đối soát. */
    PaymentStatusType reconcile(Long paymentId);
}
