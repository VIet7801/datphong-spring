package com.vn.datphong.service;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.AuditLogSearchResponse;

public interface AuditLogService {
    AuditLogSearchResponse search(AuditLogSearchRequest request);
}