package com.vn.datphong.config;

import com.vn.datphong.security.*;
import com.vn.datphong.service.Impl.CustomUserDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(CustomUserDetailsService details, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwt, CustomUserDetailsService details) {
        return new JwtAuthenticationFilter(jwt, details);
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    // Các filter chỉ chạy trong security chain, không đăng ký lần hai ở servlet container.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdRegistration(RequestIdFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
            JwtAuthenticationFilter jwt, RequestIdFilter requestIds) {
        return http.csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provider)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) ->
                                JsonErrorWriter.write(res, 401, "UNAUTHORIZED", "Chưa xác thực"))
                        .accessDeniedHandler((req, res, ex) ->
                                JsonErrorWriter.write(res, 403, "FORBIDDEN", "Không đủ quyền truy cập")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, AuthEndpoints.PUBLIC_POSTS.toArray(String[]::new)).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit-logs", "/api/admin/audit-logs")
                            .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPPORT")
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestIds, JwtAuthenticationFilter.class)
                .build();
    }
}
