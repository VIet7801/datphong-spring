package com.vn.datphong.controller;

import com.vn.datphong.dto.request.ProfileUpdateRequest;
import com.vn.datphong.dto.request.PasswordChangeRequest;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Hồ sơ cá nhân")
public class UserProfileController {
    private final UserProfileService service;

    @GetMapping
    public ApiResponse<UserProfileResponse> get() {
        return ApiResponse.success(service.getMyProfile());
    }

    @PatchMapping
    public ApiResponse<UserProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(service.updateMyProfile(request));
    }

    @PatchMapping("/password")
    public ApiResponse<UserProfileResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return ApiResponse.success(service.changePassword(request));
    }
}
