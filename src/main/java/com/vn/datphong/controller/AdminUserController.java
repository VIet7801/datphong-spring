package com.vn.datphong.controller;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.service.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - User")
public class AdminUserController {
    private final AdminUserService service;

    @GetMapping
    public ApiResponse<SliceResponse<AdminUserResponse>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.search(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PatchMapping("/{id}/profile")
    public ApiResponse<AdminUserResponse> updateProfile(@PathVariable @Positive Long id,
            @Valid @RequestBody AdminProfileUpdateRequest request) {
        return ApiResponse.success(service.updateProfile(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> changeStatus(@PathVariable @Positive Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success(service.changeStatus(id, request));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<AdminUserResponse> replaceRoles(@PathVariable @Positive Long id,
            @Valid @RequestBody UserRolesUpdateRequest request) {
        return ApiResponse.success(service.replaceRoles(id, request));
    }
}
