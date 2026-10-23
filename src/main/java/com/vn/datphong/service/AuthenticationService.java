package com.vn.datphong.service;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.security.*;
import com.vn.datphong.service.identity.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private final AuthenticationManager authenticationManager;
    private final AccountSessionService sessions;
    private final AccountRegistrationService registration;
    private final AuthRateLimiter limiter;

    public RegistrationResponse register(RegisterRequest request, String ip) {
        limiter.check("register", request.getEmail(), ip);
        try {
            return registration.register(request);
        } catch (DataIntegrityViolationException race) {
            // Transaction đã rollback. Không đẩy exception DB có thể chứa hash/token ra log.
            throw DomainException.conflict("ACCOUNT_ALREADY_EXISTS", "Email hoặc username đã được sử dụng");
        }
    }

    public AuthTokenResponse login(LoginRequest request, String ip) {
        String identity = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail() : request.getUsername().trim();
        limiter.check("login", identity.toLowerCase(Locale.ROOT), ip);
        try {
            if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new BadCredentialsException("Invalid credentials");
            }
            var authenticated = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identity, request.getPassword()));
            AuthTokenResponse result = sessions.open((AccountPrincipal) authenticated.getPrincipal());
            log.info("AUTH_LOGIN_SUCCEEDED userId={} traceId={}", result.user().id(), MDC.get("traceId"));
            return result;
        } catch (AuthenticationException failure) {
            log.info("AUTH_LOGIN_FAILED traceId={}", MDC.get("traceId"));
            throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
        }
    }

    public RegistrationResponse verify(VerifyEmailRequest request, String ip) {
        limiter.check("verify", request.getEmail(), ip);
        return registration.verify(request.getEmail(), request.getToken());
    }

    public void resend(String email, String ip) {
        limiter.check("resend", email, ip);
        registration.resend(email);
    }

    public AuthTokenResponse refresh(String token, String ip) {
        limiter.check("refresh", TokenSecrets.hash(token), ip);
        return sessions.refresh(token);
    }

    public void logout(String token, String ip) {
        limiter.check("logout", TokenSecrets.hash(token), ip);
        sessions.logout(token);
    }
}
