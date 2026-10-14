package com.vn.datphong.service;


import com.vn.datphong.dto.request.LoginRequest;
import com.vn.datphong.dto.response.LoginResponse;
import com.vn.datphong.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {

        // 1. Đóng gói username và password để yêu cầu xác thực
        Authentication authenticationRequest =
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                );

        // 2. Spring Security xác thực tài khoản và mật khẩu
        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        // 3. Xác thực thành công thì sinh JWT
        String accessToken =
                jwtTokenProvider.generateToken(authentication);

        // 4. Trả token cho Controller
        return new LoginResponse(accessToken, "Bearer");
    }
}