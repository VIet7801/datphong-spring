package com.vn.datphong.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id;
        try {
            id = UUID.fromString(request.getHeader("X-Request-ID")).toString();
        } catch (IllegalArgumentException | NullPointerException invalid) {
            id = UUID.randomUUID().toString();
        }
        MDC.put("traceId", id);
        response.setHeader("X-Request-ID", id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
