package com.vn.datphong.service.identity;

import com.vn.datphong.config.AuthProperties;
import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.RegistrationResponse;
import com.vn.datphong.entity.*;
import com.vn.datphong.enums.*;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.*;
import com.vn.datphong.security.TokenSecrets;
import com.vn.datphong.service.AuditLogWriter;
import com.vn.datphong.util.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountRegistrationService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final VerificationTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final VerificationMailService mail;
    private final AccountSessionService sessions;
    private final AuditLogWriter audit;
    private final Clock clock;
    private final AuthProperties properties;

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        User user = create(request.getEmail(), request.getUsername(), request.getFullName(),
                request.getPhone(), request.getGender(), request.getPassword());
        audit.systemAccountEvent("USER_REGISTERED", user, "Tự đăng ký; chờ xác minh email");
        return new RegistrationResponse(user.getStatus(), "Đã đăng ký; hãy kiểm tra email xác minh");
    }

    @Transactional
    public User createByAdmin(AdminUserCreateRequest request) {
        return create(request.getEmail(), request.getUsername(), request.getFullName(),
                request.getPhone(), request.getGender(), request.getPassword());
    }

    private User create(String address, String username, String fullName, String phone, Boolean gender, String password) {
        String email = EmailAddress.normalize(address);
        String loginName = username == null || username.isBlank() ? email : username.trim();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw DomainException.badRequest("PASSWORD_TOO_LONG", "Mật khẩu vượt giới hạn 72 byte UTF-8 của BCrypt");
        }
        if (users.findByNormalizedEmail(email).isPresent()) {
            throw DomainException.conflict("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng");
        }
        if (users.existsByUsername(loginName)) {
            throw DomainException.conflict("USERNAME_ALREADY_EXISTS", "Username đã được sử dụng");
        }
        Role customer = roles.findByCode("ROLE_CUSTOMER")
                .orElseThrow(() -> DomainException.conflict("RBAC_NOT_INITIALIZED", "Chưa khởi tạo ROLE_CUSTOMER"));
        User user = users.saveAndFlush(User.builder().email(email).username(loginName).fullName(fullName.trim())
                .phone(phone).gender(gender).password(encoder.encode(password))
                .active(false).status(UserStatus.PENDING_VERIFICATION).roles(new HashSet<>(Set.of(customer))).build());
        issue(user);
        return user;
    }

    @Transactional
    public void resend(String address) {
        Long found = users.findIdByNormalizedEmail(EmailAddress.normalize(address)).orElse(null);
        if (found == null) return;
        User user = users.findByIdForUpdate(found).orElseThrow();
        if (user.getEmailVerifiedAt() != null) return;
        VerificationToken previous = latest(user).orElse(null);
        if (previous != null && previous.getCreatedAt().plus(properties.getResendCooldown()).isAfter(clock.instant())) return;
        if (previous != null && previous.getUsedAt() == null) previous.setRevokedAt(clock.instant());
        issue(user);
    }

    // Lưu số lần sai và trạng thái thu hồi trước khi trả lỗi xác minh.
    @Transactional(noRollbackFor = VerificationRejected.class)
    public RegistrationResponse verify(String address, String raw) {
        Long found = users.findIdByNormalizedEmail(EmailAddress.normalize(address)).orElse(null);
        if (found == null) throw new VerificationRejected("VERIFICATION_INVALID");
        User user = users.findByIdForUpdate(found).orElseThrow();
        VerificationToken token = latest(user).orElseThrow(() -> new VerificationRejected("VERIFICATION_INVALID"));
        if (token.getUsedAt() != null || token.getRevokedAt() != null) throw new VerificationRejected("VERIFICATION_INVALID");
        if (!token.getExpiresAt().isAfter(clock.instant())) throw new VerificationRejected("VERIFICATION_EXPIRED");
        if (!TokenSecrets.matches(raw, token.getTokenHash())) {
            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= properties.getVerificationMaxAttempts()) token.setRevokedAt(clock.instant());
            throw new VerificationRejected("VERIFICATION_INVALID");
        }
        token.setUsedAt(clock.instant());
        user.setEmailVerifiedAt(clock.instant());
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
            user.setActive(true);
        }
        // Xác minh không tự mở khóa tài khoản LOCKED/IN_ACTIVE.
        sessions.revokeAll(user);
        audit.systemAccountEvent("USER_EMAIL_VERIFIED", user, "Xác minh email bằng mã dùng một lần");
        return new RegistrationResponse(user.getStatus(), "Email đã được xác minh");
    }

    private Optional<VerificationToken> latest(User user) {
        return tokens.findFirstByUser_IdAndPurposeOrderByIdDesc(user.getId(), VerificationPurpose.EMAIL_VERIFICATION);
    }

    private void issue(User user) {
        String raw = TokenSecrets.create();
        VerificationToken token = tokens.saveAndFlush(VerificationToken.builder().user(user)
                .purpose(VerificationPurpose.EMAIL_VERIFICATION).tokenHash(TokenSecrets.hash(raw))
                .expiresAt(clock.instant().plus(properties.getVerificationTtl())).build());
        mail.enqueue(user, token, raw);
    }

    private static class VerificationRejected extends DomainException {
        VerificationRejected(String code) {
            super(org.springframework.http.HttpStatus.BAD_REQUEST, code, "Mã xác minh không hợp lệ hoặc đã hết hạn");
        }
    }
}
