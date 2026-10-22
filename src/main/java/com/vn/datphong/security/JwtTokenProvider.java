package com.vn.datphong.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:900000}") long expirationMs
    ) {
        // Secret phải là chuỗi Base64 đủ dài để dùng HS256
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.expirationMs = expirationMs;
    }

    // Tạo JWT sau khi người dùng đăng nhập thành công
    public String generateToken(Authentication authentication) {

        String username = authentication.getName();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Permissions::isRoleAuthority)
                        .toList())
                .claim("tokenVersion", authentication.getPrincipal() instanceof AccountPrincipal principal
                        ? principal.getTokenVersion() : 0L)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // Đọc username từ JWT và xác minh chữ ký
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractTokenVersion(String token) {
        Object version = parseClaims(token).get("tokenVersion");
        return version instanceof Number number ? number.longValue() : null;
    }

    // Kiểm tra JWT có hợp lệ và đúng người dùng không
    public boolean isTokenValid(
            String token
    ) {
        try {
            String username = extractUsername(token);
            return username != null && !username.isBlank();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
