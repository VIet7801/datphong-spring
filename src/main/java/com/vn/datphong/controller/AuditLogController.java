package com.vn.datphong.controller;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.AuditLogSearchResponse;
import com.vn.datphong.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/v1/admin/audit-logs", "/api/admin/audit-logs"})
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "ADMIN/SUPPORT có AUDIT_READ theo UC-19")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogSearchResponse>> search(
            @Valid @ParameterObject @ModelAttribute AuditLogSearchRequest request
    ) {
        AuditLogSearchResponse result = auditLogService.search(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
