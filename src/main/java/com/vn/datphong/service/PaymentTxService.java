package com.vn.datphong.service;

import com.vn.datphong.entity.Payment;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.PaymentStatusType;

import java.util.EnumSet;
import java.util.Set;

public interface PaymentTxService {

    /** Trạng thái chặn tạo payment mới (khớp partial unique index uq_payments_one_active_per_booking). */
    public static final Set<PaymentStatusType> ACTIVE = EnumSet.of(PaymentStatusType.PENDING, PaymentStatusType.INITIATED,
            PaymentStatusType.PROCESSING, PaymentStatusType.TIMED_OUT, PaymentStatusType.SUCCEEDED,
            PaymentStatusType.NEEDS_ATTENTION);


    /** @return id payment; bản ghi cũ nếu cùng key (phát lại). */
    Payment begin(User user, Long bookingId, String accountNo, String key, String requestHash);

    void markProcessing(Long paymentId, String reference);

    void markTimedOut(Long paymentId);
}
