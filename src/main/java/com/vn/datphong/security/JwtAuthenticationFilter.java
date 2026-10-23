package com.vn.datphong.security;

import com.vn.datphong.service.Impl.CustomUserDetailsService;
import io.jsonwebtoken.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwt;
    private final CustomUserDetailsService details;

    public JwtAuthenticationFilter(JwtTokenProvider jwt, CustomUserDetailsService details) {
        this.jwt = jwt;
        this.details = details;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // Login/refresh không bị token access hết hạn trên client chặn trước controller.
        return "POST".equals(request.getMethod()) && AuthEndpoints.PUBLIC_POSTS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwt.parse(header.substring(7));
            var principal = (AccountPrincipal) details.loadUserById(Long.valueOf(claims.getSubject()));
            Object version = claims.get("tokenVersion");
            if (!principal.isEnabled() || !principal.isAccountNonLocked()
                    || !(version instanceof Number number)
                    || !Objects.equals(number.longValue(), principal.getTokenVersion())) {
                throw new IllegalArgumentException("Session revoked");
            }
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException invalid) {
            SecurityContextHolder.clearContext();
            JsonErrorWriter.write(response, 401, "UNAUTHORIZED", "Chưa xác thực hoặc phiên đã hết hạn");
            return;
        }
        chain.doFilter(request, response);
    }
}
