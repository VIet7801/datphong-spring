package com.vn.datphong.config;

import com.vn.datphong.security.JwtAuthenticationFilter;

import com.vn.datphong.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Cấu hình bảo mật được tạo khi khởi động, không phải điểm xử lý từng request.
 * Luồng runtime: JWT filter -> kiểm tra URL -> controller -> proxy kiểm tra quyền service.
 * Xem docs/REQUEST_FLOW.md để theo dõi đầy đủ request/response.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Provider dùng UserDetailsService để tải user và PasswordEncoder để đối chiếu mật khẩu lúc login.
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Tạo filter thành Bean, không gắn @Component trên class filter.
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtProvider,
            UserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(
                jwtProvider,
                userDetailsService
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider provider,
            JwtAuthenticationFilter jwtFilter
    )  {

        // Chưa đăng nhập hoặc JWT không hợp lệ: trả HTTP 401.
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, exception) ->
                        response.sendError(401, "Unauthorized");

        // Đã đăng nhập nhưng không đủ quyền: trả HTTP 403.
        AccessDeniedHandler accessDeniedHandler =
                (request, response, exception) ->
                        response.sendError(403, "Forbidden");

        http.csrf(csrf -> csrf.disable())

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Mỗi request cần tự gửi JWT; không duy trì đăng nhập bằng HTTP session.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationProvider(provider)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // UC-19: mở riêng GET Audit Log cho ADMIN/SUPPORT; service kiểm tra AUDIT_READ.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/auth/login",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/admin/audit-logs",
                                "/api/admin/audit-logs"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPPORT")
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**")
                        .hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )

                // Xác thực JWT trước khi chuỗi filter quyết định request có được đi tiếp không.
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
