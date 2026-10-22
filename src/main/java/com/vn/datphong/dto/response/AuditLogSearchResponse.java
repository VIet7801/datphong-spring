package com.vn.datphong.dto.response;

import java.util.List;

public record AuditLogSearchResponse(
        int page,
        int size,
        boolean hasNext,
        List<AuditLogResponse> items
) {
}