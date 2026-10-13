package com.vn.datphong.security;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Bước 1: Đọc header Authorization.
        String authorizationHeader =
                request.getHeader("Authorization");

        log.info("Request URI: {}", request.getRequestURI());
        log.info("Authorization header tồn tại: {}",
                authorizationHeader != null);
        // Không có Bearer token thì chuyển sang filter tiếp theo.
        // Endpoint công khai vẫn có thể truy cập bình thường.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Bước 2: Tách JWT khỏi chuỗi "Bearer <token>".
        String token = authorizationHeader.substring(7);
        try {
            if (!jwtTokenProvider.isTokenValid(token)) {
                log.warn("JWT không hợp lệ hoặc hết hạn");

                SecurityContextHolder.clearContext();
                response.sendError(401, "JWT không hợp lệ hoặc đã hết hạn");
                return;
            }

            String username = jwtTokenProvider.extractUsername(token);
            log.debug("JWT hợp lệ, username={}", username);

             //Giữ nguyên phần code còn lại của bạn.

             //Chỉ xác thực nếu request chưa có Authentication
            if (SecurityContextHolder.getContext()
                                .getAuthentication() == null) {

                            // Bước 5: Tải thông tin tài khoản và quyền hiện tại.
                            UserDetails userDetails =
                                    userDetailsService.loadUserByUsername(username);

                            // Không cho tài khoản bị vô hiệu hóa đăng nhập bằng token cũ.
                            if (!userDetails.isEnabled()
                                    || !userDetails.isAccountNonLocked()
                                    || !userDetails.isAccountNonExpired()
                                    || !userDetails.isCredentialsNonExpired()) {

                                SecurityContextHolder.clearContext();
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Tài khoản không được phép truy cập"
                                );
                                return;
                            }

                            // Bước 6: Tạo Authentication đã được xác minh bằng JWT.
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            // Bước 7: Lưu Authentication vào SecurityContext.
                            SecurityContextHolder.getContext()
                                    .setAuthentication(authentication);
                        }

                    } catch (JwtException | IllegalArgumentException exception) {
                        SecurityContextHolder.clearContext();

                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "JWT không hợp lệ"
                        );
                        return;

                    } catch (org.springframework.security.core.userdetails
                                     .UsernameNotFoundException exception) {

                        SecurityContextHolder.clearContext();

                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Tài khoản không còn tồn tại"
                        );
                        return;
                    }

                    // Bước 8: Tiếp tục xử lý request.
                    filterChain.doFilter(request, response);
                }
            }
