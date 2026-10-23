package com.vn.datphong.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long expirationMs;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration-ms:900000}") long expirationMs, Clock clock) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    public JwtTokenProvider(String secret, long expirationMs) {
        this(secret, expirationMs, Clock.systemUTC());
    }

    public String generateToken(Authentication authentication) {
        return generateToken(authentication, clock.instant().plusMillis(expirationMs));
    }

    public String generateToken(Authentication authentication, Instant sessionExpires) {
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        Instant now = clock.instant();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(principal.getUserId().toString())
                .claim("roles", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(Permissions::isRoleAuthority).toList())
                .claim("tokenVersion", principal.getTokenVersion())
                .issuedAt(Date.from(now)).expiration(Date.from(expiration(sessionExpires)))
                .signWith(secretKey).compact();
    }

    public long expiresIn(Instant sessionExpires) {
        return Math.max(0, Duration.between(clock.instant(), expiration(sessionExpires)).toSeconds());
    }

    private Instant expiration(Instant sessionExpires) {
        Instant expiry = clock.instant().plusMillis(expirationMs);
        return sessionExpires.isBefore(expiry) ? sessionExpires : expiry;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).clock(() -> Date.from(clock.instant()))
                .build().parseSignedClaims(token).getPayload();
    }

    public Long extractTokenVersion(String token) {
        Object value = parse(token).get("tokenVersion");
        return value instanceof Number number ? number.longValue() : null;
    }

    public boolean isTokenValid(String token) {
        try {
            return Long.parseLong(parse(token).getSubject()) > 0;
        } catch (JwtException | IllegalArgumentException failure) {
            return false;
        }
    }
}
