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

/**
 * Điểm bắt đầu đọc code cho một request có JWT, trước khi vào controller.
 * Xác thực token + trạng thái tài khoản rồi đặt Authentication cho request hiện tại.
 * Filter có thể trả 401 ngay tại đây; các lỗi này không đi qua controller advice.
 * Xem docs/REQUEST_FLOW.md để đối chiếu toàn bộ luồng.
 */
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
        // API yêu cầu đăng nhập sẽ bị filter phía sau từ chối nếu vẫn chưa có Authentication.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Bước 2: Tách JWT khỏi chuỗi "Bearer <token>".
        String token = authorizationHeader.substring(7);
        try {
            // Bước 3: Xác minh chữ ký/hạn JWT; token không hợp lệ thì dừng trước controller.
            if (!jwtTokenProvider.isTokenValid(token)) {
                log.warn("JWT không hợp lệ hoặc hết hạn");

                SecurityContextHolder.clearContext();
                response.sendError(401, "JWT không hợp lệ hoặc đã hết hạn");
                return;
            }

            String username = jwtTokenProvider.extractUsername(token);
            log.debug("JWT hợp lệ, username={}", username);

             // Bước 4: Đọc username từ JWT và chỉ xác thực khi context chưa có danh tính.
            if (SecurityContextHolder.getContext()
                                .getAuthentication() == null) {

                            // Bước 5: Tải user/quyền/trạng thái hiện tại từ DB qua CustomUserDetailsService.
                            // Authorities dùng bên dưới là từ DB, không lấy trực tiếp từ claim roles của JWT.
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

                            // Lock/unlock hoặc đổi role làm tokenVersion tăng: JWT cũ không dùng lại được.
                            if (!(userDetails instanceof AccountPrincipal principal)
                                    || !java.util.Objects.equals(
                                            jwtTokenProvider.extractTokenVersion(token), principal.getTokenVersion())) {
                                SecurityContextHolder.clearContext();
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Phiên đăng nhập đã bị thu hồi; hãy đăng nhập lại");
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

                    // Bước 8: Chuyển tiếp sang các filter còn lại, rồi Spring MVC mới gọi controller.
                    // Khi các bước phía sau xử lý xong, HTTP response được gửi ngược về FE.
                    filterChain.doFilter(request, response);
                }
            }
