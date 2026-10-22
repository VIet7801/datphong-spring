package com.vn.datphong.controller;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.service.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@Tag(name = "Admin - Permission")
public class AdminPermissionController {
    private final PermissionService service;

    @GetMapping
    public ApiResponse<SliceResponse<PermissionResponse>> search(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.search(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PermissionResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody PermissionUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id, @Valid @RequestBody VersionedReasonRequest request) {
        service.delete(id, request);
    }
}
