package com.vn.datphong.controller;

import com.vn.datphong.dto.request.LoginRequest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.LoginResponse;
import com.vn.datphong.service.AuthenticationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                authenticationService.login(request);

        return ResponseEntity.ok(
                ApiResponse.customsuccess(
                        "Đăng nhập thành công",
                        response
                )
        );
    }
}