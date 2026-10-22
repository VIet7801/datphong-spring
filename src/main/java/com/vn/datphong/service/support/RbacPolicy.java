package com.vn.datphong.service.support;

import com.vn.datphong.entity.Role;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.repository.*;
import com.vn.datphong.security.CurrentUser;
import com.vn.datphong.security.Permissions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RbacPolicy {
    public static final Set<String> SYSTEM_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_HOST", "ROLE_SUPPORT", "ROLE_USER");
    private final RoleRepository roles;
    private final RolePermissionRepository rolePermissions;
    private final UserRepository users;
    private final CurrentUser currentUser;

    /** Caller phải có transaction. Cùng một khóa được dùng cho mọi thay đổi RBAC. */
    public void lockAdministration() {
        roles.findByCodeForUpdate("ROLE_ADMIN")
                .orElseThrow(() -> new BusinessException(503, "Chưa khởi tạo danh mục RBAC"));
    }

    public void requireNotSelf(User target) {
        if (Objects.equals(currentUser.require().getId(), target.getId())) {
            throw new AccessDeniedException("Không tự thay đổi trạng thái hoặc role của mình");
        }
    }

    public void preserveLastAdmin(User target, boolean remainsActiveAdmin) {
        boolean currentlyActiveAdmin = Boolean.TRUE.equals(target.getActive())
                && target.getStatus() == UserStatus.ACTIVE && hasAdminRole(target);
        if (currentlyActiveAdmin && !remainsActiveAdmin
                && users.countActiveAdmins(UserStatus.ACTIVE) <= 1) {
            throw new BusinessException(409, "Không thể vô hiệu hóa ADMIN hoạt động cuối cùng");
        }
    }

    public static boolean hasAdminRole(User user) {
        return user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getCode()));
    }

    public void requireNotOwnRole(Role role) {
        if (currentUser.require().getRoles().stream().anyMatch(r -> Objects.equals(r.getId(), role.getId()))) {
            throw new AccessDeniedException("Không thay đổi permission của role đang gắn với chính mình");
        }
    }

    public void requireDelegablePermissions(Set<String> codes) {
        Set<String> owned = new HashSet<>(rolePermissions.findPermissionCodesByUserId(currentUser.require().getId()));
        if (codes.stream().anyMatch(c -> !Permissions.isPermissionAuthority(c)) || !owned.containsAll(codes)) {
            throw new AccessDeniedException("Không cấp hoặc thu hồi permission vượt phạm vi được ủy quyền");
        }
    }

    public void requireDelegableRoles(Collection<Role> changedRoles) {
        Set<String> codes = changedRoles.stream()
                .flatMap(r -> rolePermissions.findByRole_Id(r.getId()).stream())
                .map(rp -> rp.getPermission().getCode()).collect(Collectors.toSet());
        requireDelegablePermissions(codes);
    }
}
