package com.vn.datphong.controller;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.AuditLogSearchResponse;
import com.vn.datphong.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogSearchResponse>> search(
            @Valid @ModelAttribute AuditLogSearchRequest request
    ) {
        AuditLogSearchResponse result = auditLogService.search(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}