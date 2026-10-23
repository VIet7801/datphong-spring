package com.vn.datphong.service.identity;

import com.vn.datphong.config.AuthProperties;
import com.vn.datphong.dto.response.AuthTokenResponse;
import com.vn.datphong.entity.*;
import com.vn.datphong.repository.*;
import com.vn.datphong.security.*;
import com.vn.datphong.service.AuditLogWriter;
import com.vn.datphong.service.Impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountSessionService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final CustomUserDetailsService details;
    private final JwtTokenProvider jwt;
    private final AuditLogWriter audit;
    private final Clock clock;
    private final AuthProperties properties;

    @Transactional
    public AuthTokenResponse open(AccountPrincipal authenticated) {
        User user = users.findByIdForUpdate(authenticated.getUserId()).orElseThrow(this::invalid);
        requireUsable(user);
        if (!java.util.Objects.equals(user.getTokenVersion(), authenticated.getTokenVersion())) throw invalid();
        String raw = TokenSecrets.create();
        RefreshToken token = RefreshToken.builder().user(user).familyId(UUID.randomUUID())
                .tokenHash(TokenSecrets.hash(raw)).expiresAt(clock.instant().plus(properties.getRefreshTtl())).build();
        refreshTokens.save(token);
        audit.accountEvent("USER_LOGIN_SUCCEEDED", user, "Đăng nhập");
        return response(user, raw, token.getExpiresAt());
    }

    // Reuse phải thu hồi phiên rồi mới trả 401; không rollback phần thu hồi này.
    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthTokenResponse refresh(String raw) {
        String hash = TokenSecrets.hash(raw);
        User user = ownerForUpdate(hash);
        RefreshToken previous = refreshTokens.findByTokenHash(hash).orElseThrow(this::invalid);
        Instant now = clock.instant();
        if (!previous.getExpiresAt().isAfter(now)) throw invalid();
        if (previous.getRevokedAt() != null) {
            // Chỉ xử lý reuse khi family còn sống. Token cũ đã đóng không được phá phiên login mới.
            if (previous.getReplacedBy() != null
                    && refreshTokens.existsByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(previous.getFamilyId(), now)) {
                revokeAll(user);
                audit.accountEvent("REFRESH_TOKEN_REUSE_DETECTED", user, "Thu hồi phiên do refresh token bị dùng lại");
            }
            throw invalid();
        }
        requireUsable(user);
        String nextRaw = TokenSecrets.create();
        RefreshToken next = refreshTokens.save(RefreshToken.builder().user(user).familyId(previous.getFamilyId())
                .tokenHash(TokenSecrets.hash(nextRaw)).expiresAt(previous.getExpiresAt()).build());
        previous.setRevokedAt(now);
        previous.setReplacedBy(next);
        audit.accountEvent("USER_SESSION_REFRESHED", user, "Làm mới phiên đăng nhập");
        return response(user, nextRaw, next.getExpiresAt());
    }

    @Transactional
    public void logout(String raw) {
        String hash = TokenSecrets.hash(raw);
        Long id = refreshTokens.findUserIdByHash(hash).orElse(null);
        if (id == null) return;
        User user = users.findByIdForUpdate(id).orElse(null);
        if (user == null) return;
        RefreshToken token = refreshTokens.findByTokenHash(hash).orElse(null);
        if (token == null || token.getRevokedAt() != null || !token.getExpiresAt().isAfter(clock.instant())) return;
        revokeAll(user);
        audit.accountEvent("USER_LOGGED_OUT", user, "Đăng xuất toàn bộ thiết bị");
    }

    /** Caller phải khóa User trước. Dùng chung cho logout, khóa user, đổi mật khẩu và đổi role. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0L : user.getTokenVersion()) + 1);
        refreshTokens.revokeAll(user.getId(), clock.instant());
    }

    private User ownerForUpdate(String hash) {
        Long id = refreshTokens.findUserIdByHash(hash).orElseThrow(this::invalid);
        return users.findByIdForUpdate(id).orElseThrow(this::invalid);
    }

    private void requireUsable(User user) {
        if (!Boolean.TRUE.equals(user.getActive())
                || user.getStatus() != com.vn.datphong.enums.UserStatus.ACTIVE || user.getEmailVerifiedAt() == null) {
            throw invalid();
        }
    }

    private AuthTokenResponse response(User user, String rawRefresh, Instant refreshExpires) {
        AccountPrincipal principal = details.principal(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var codes = principal.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
        return new AuthTokenResponse(jwt.generateToken(authentication, refreshExpires), rawRefresh, "Bearer",
                jwt.expiresIn(refreshExpires), Math.max(0, Duration.between(clock.instant(), refreshExpires).toSeconds()),
                new AuthTokenResponse.UserSummary(user.getId(), user.getFullName(), user.getUsername(),
                        codes.stream().filter(Permissions::isRoleAuthority).collect(Collectors.toSet()),
                        codes.stream().filter(Permissions::isPermissionAuthority).collect(Collectors.toSet())));
    }

    private BadCredentialsException invalid() {
        return new BadCredentialsException("Phiên đăng nhập không hợp lệ");
    }
}
