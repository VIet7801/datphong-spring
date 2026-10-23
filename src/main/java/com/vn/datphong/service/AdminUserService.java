package com.vn.datphong.service;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.entity.*;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.exception.*;
import com.vn.datphong.mapper.UserViewMapper;
import com.vn.datphong.repository.*;
import com.vn.datphong.service.support.*;
import com.vn.datphong.service.identity.AccountRegistrationService;
import com.vn.datphong.service.identity.AccountSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditLogWriter audit;
    private final RbacPolicy policy;
    private final AccountRegistrationService registration;
    private final AccountSessionService sessions;
    private final AdminAccountImpact impact;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_READ')")
    public SliceResponse<AdminUserResponse> search(String keyword, UserStatus status, int page, int size) {
        // Chuỗi rỗng tìm tất cả; không bind null vào lower/concat của PostgreSQL.
        return SliceResponse.from(users.search(keyword == null ? "" : keyword.trim(),
                status, PageRequest.of(page, size, Sort.by("id").descending())), UserViewMapper::admin);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_READ')")
    public AdminUserResponse get(Long id) {
        return UserViewMapper.admin(requireUser(id)).withWarnings(impact.warnings(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_MANAGE')")
    public AdminUserResponse create(AdminUserCreateRequest request) {
        User user = registration.createByAdmin(request);
        // Không ghi mật khẩu/hash, email hay phone vào event.
        audit.write("USER_CREATED", "USER", user.getId(), Map.of(),
                Map.of("status", user.getStatus().name(), "roles", roleCodes(user)), request.getReason());
        return UserViewMapper.admin(user);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_MANAGE')")
    public AdminUserResponse updateProfile(Long id, AdminProfileUpdateRequest request) {
        User user = requireUser(id);
        policy.requireManageableUser(user);
        ChangeTracker.requireVersion(user.getVersion(), request.getVersion());
        ChangeTracker changes = UserProfileService.applyProfile(user, request);
        if (changes.hasChanges()) {
            users.flush();
            audit.write("USER_PROFILE_UPDATED_BY_ADMIN", "USER", id,
                    changes.before(), changes.after(), request.getReason());
        }
        return UserViewMapper.admin(user);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_MANAGE')")
    public AdminUserResponse changeStatus(Long id, UserStatusUpdateRequest request) {
        policy.lockAdministration();
        User user = requireUserForUpdate(id);
        ChangeTracker.requireVersion(user.getVersion(), request.getVersion());
        policy.requireNotSelf(user);
        policy.requireManageableUser(user);
        // PENDING_VERIFICATION thuộc luồng đăng ký/xác minh, không phải thao tác khóa/mở khóa.
        if (request.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestExcept("Chỉ quản trị ACTIVE, IN_ACTIVE hoặc LOCKED");
        }
        boolean active = request.getStatus() == UserStatus.ACTIVE;
        if (active && user.getEmailVerifiedAt() == null) {
            throw new BusinessException(409, "EMAIL_NOT_VERIFIED", "Cần xác minh email trước khi kích hoạt");
        }
        policy.preserveLastAdmin(user, active && RbacPolicy.hasAdminRole(user));
        if (user.getStatus() != request.getStatus() || !Objects.equals(user.getActive(), active)) {
            Map<String, Object> before = statusSnapshot(user);
            user.setStatus(request.getStatus());
            user.setActive(active);
            sessions.revokeAll(user);
            users.flush();
            audit.write("USER_STATUS_CHANGED", "USER", id, before, statusSnapshot(user), request.getReason());
        }
        return UserViewMapper.admin(user).withWarnings(impact.warnings(user.getId()));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('USER_ROLE_MANAGE')")
    public AdminUserResponse replaceRoles(Long id, UserRolesUpdateRequest request) {
        policy.lockAdministration();
        User user = requireUserForUpdate(id);
        ChangeTracker.requireVersion(user.getVersion(), request.getVersion());
        policy.requireNotSelf(user);
        policy.requireManageableUser(user);
        List<Role> selected = roles.findAllById(request.getRoleIds());
        if (selected.size() != request.getRoleIds().size()) {
            throw new BadRequestExcept("Có roleId không tồn tại");
        }
        Set<Long> previousIds = user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        if (previousIds.equals(request.getRoleIds())) return UserViewMapper.admin(user);
        List<Role> changed = new ArrayList<>();
        user.getRoles().stream().filter(r -> !request.getRoleIds().contains(r.getId())).forEach(changed::add);
        selected.stream().filter(r -> !previousIds.contains(r.getId())).forEach(changed::add);
        policy.requireDelegableRoles(changed);
        boolean remainsAdmin = Boolean.TRUE.equals(user.getActive()) && user.getStatus() == UserStatus.ACTIVE
                && selected.stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getCode()));
        policy.preserveLastAdmin(user, remainsAdmin);
        Set<String> before = roleCodes(user);
        user.setRoles(new HashSet<>(selected));
        sessions.revokeAll(user);
        users.flush();
        audit.write("USER_ROLES_CHANGED", "USER", id,
                Map.of("roles", before), Map.of("roles", roleCodes(user)), request.getReason());
        return UserViewMapper.admin(user).withWarnings(impact.warnings(user.getId()));
    }

    private User requireUserForUpdate(Long id) {
        return users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundExcept("User không tồn tại"));
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("User không tồn tại"));
    }

    private Set<String> roleCodes(User user) {
        return user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    }

    private Map<String, Object> statusSnapshot(User user) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", user.getStatus());
        snapshot.put("active", user.getActive());
        return snapshot;
    }
}
