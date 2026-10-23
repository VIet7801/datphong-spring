package com.vn.datphong.controller;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.service.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@SecurityRequirements
public class AuthenticationController {
    private final AuthenticationService service;

    @ModelAttribute
    public void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.register(body, request.getRemoteAddr()));
    }

    @PostMapping("/verify")
    public ApiResponse<RegistrationResponse> verify(@Valid @RequestBody VerifyEmailRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.verify(body, request.getRemoteAddr()));
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Map<String, String>> resend(@Valid @RequestBody ResendVerificationRequest body, HttpServletRequest request) {
        service.resend(body.getEmail(), request.getRemoteAddr());
        return ApiResponse.success(Map.of("message", "Nếu tài khoản cần xác minh, email sẽ được gửi"));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.login(body, request.getRemoteAddr()));
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.refresh(body.getRefreshToken(), request.getRemoteAddr()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest body, HttpServletRequest request) {
        service.logout(body.getRefreshToken(), request.getRemoteAddr());
    }
}
