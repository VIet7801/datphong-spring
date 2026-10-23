package com.vn.datphong.service;

import com.vn.datphong.dto.request.ProfileUpdateRequest;
import com.vn.datphong.dto.request.PasswordChangeRequest;
import com.vn.datphong.dto.response.UserProfileResponse;
import com.vn.datphong.entity.User;
import com.vn.datphong.mapper.UserViewMapper;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.security.CurrentUser;
import com.vn.datphong.service.support.ChangeTracker;
import com.vn.datphong.exception.BadRequestExcept;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vn.datphong.service.identity.AccountSessionService;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileService {
    private final CurrentUser currentUser;
    private final UserRepository users;
    private final AuditLogWriter audit;
    private final PasswordEncoder passwordEncoder;
    private final AccountSessionService sessions;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        return UserViewMapper.profile(currentUser.require());
    }

    @Transactional
    public UserProfileResponse updateMyProfile(ProfileUpdateRequest request) {
        // Không nhận targetUserId: luôn tải tài khoản từ principal đã xác thực.
        User user = currentUser.require();
        ChangeTracker.requireVersion(user.getVersion(), request.getVersion());
        ChangeTracker changes = applyProfile(user, request);
        if (changes.hasChanges()) {
            users.flush();
            audit.write("USER_PROFILE_UPDATED", "USER", user.getId(),
                    changes.before(), changes.after(), "Người dùng cập nhật hồ sơ cá nhân");
        }
        return UserViewMapper.profile(user);
    }

    static ChangeTracker applyProfile(User user, ProfileUpdateRequest request) {
        ChangeTracker changes = new ChangeTracker();
        changes.set("fullName", user.getFullName(), request.getFullName(), user::setFullName);
        changes.set("phone", user.getPhone(), request.getPhone(), user::setPhone);
        changes.set("gender", user.getGender(), request.getGender(), user::setGender);
        changes.set("avatar", user.getAvatar(), request.getAvatar(), user::setAvatar);
        return changes;
    }

    @Transactional
    public UserProfileResponse changePassword(PasswordChangeRequest request) {
        User user = currentUser.requireForUpdate();
        ChangeTracker.requireVersion(user.getVersion(), request.getVersion());
        if (request.getCurrentPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestExcept("Mật khẩu hiện tại không đúng");
        }
        if (request.getNewPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestExcept("Mật khẩu vượt giới hạn 72 byte của BCrypt");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestExcept("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sessions.revokeAll(user);
        users.flush();
        audit.write("USER_PASSWORD_CHANGED", "USER", user.getId(), java.util.Map.of(),
                java.util.Map.of("passwordChanged", true), "Người dùng đổi mật khẩu");
        return UserViewMapper.profile(user);
    }
}
