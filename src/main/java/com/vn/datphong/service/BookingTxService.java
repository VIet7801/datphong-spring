package com.vn.datphong.service;

import com.vn.datphong.dto.request.BookingCreateRequest;
import com.vn.datphong.entity.User;

public interface BookingTxService {
    public static final String SCOPE_PREFIX = "booking:create:";

    Long create(User user, BookingCreateRequest req, String idempotencyKey, String requestHash);
}
