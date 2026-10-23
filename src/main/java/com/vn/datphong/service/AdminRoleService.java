package com.vn.datphong.service;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.entity.*;
import com.vn.datphong.exception.*;
import com.vn.datphong.repository.*;
import com.vn.datphong.service.support.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminRoleService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository links;
    private final UserRepository users;
    private final RbacPolicy policy;
    private final AuditLogWriter audit;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_READ')")
    public SliceResponse<AdminRoleResponse> search(int page, int size) {
        return SliceResponse.from(roles.findAllBy(PageRequest.of(page, size, Sort.by("id"))), this::response);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_READ')")
    public AdminRoleResponse get(Long id) {
        return response(requireRole(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_MANAGE')")
    public AdminRoleResponse create(RoleCreateRequest request) {
        policy.lockAdministration();
        if (roles.existsByCode(request.getCode())) throw new BusinessException(409, "Mã role đã tồn tại");
        if (permissions.existsByCode(request.getCode())) {
            throw new BusinessException(409, "Mã role không được trùng mã permission");
        }
        Role role = roles.saveAndFlush(Role.builder().code(request.getCode()).name(request.getName())
                .description(request.getDescription()).build());
        audit.write("ROLE_CREATED", "ROLE", role.getId(), Map.of(), snapshot(role), request.getReason());
        return response(role);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_MANAGE')")
    public AdminRoleResponse update(Long id, RoleUpdateRequest request) {
        policy.lockAdministration();
        Role role = requireRole(id);
        ChangeTracker.requireVersion(role.getVersion(), request.getVersion());
        if (Objects.equals(role.getName(), request.getName())
                && Objects.equals(role.getDescription(), request.getDescription())) return response(role);
        Map<String, Object> before = snapshot(role);
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        roles.flush();
        audit.write("ROLE_UPDATED", "ROLE", id, before, snapshot(role), request.getReason());
        return response(role);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_MANAGE')")
    public AdminRoleResponse replacePermissions(Long id, RolePermissionsUpdateRequest request) {
        policy.lockAdministration();
        Role role = requireRole(id);
        ChangeTracker.requireVersion(role.getVersion(), request.getVersion());
        policy.requireNotOwnRole(role);
        List<RolePermission> previous = links.findByRole_Id(id);
        Set<Long> previousIds = previous.stream().map(rp -> rp.getPermission().getId()).collect(Collectors.toSet());
        if (previousIds.equals(request.getPermissionIds())) return response(role);
        List<Permission> selected = permissions.findAllById(request.getPermissionIds());
        if (selected.size() != request.getPermissionIds().size()) {
            throw new BadRequestExcept("Có permissionId không tồn tại");
        }
        Set<String> changed = new HashSet<>();
        previous.stream().filter(rp -> !request.getPermissionIds().contains(rp.getPermission().getId()))
                .map(rp -> rp.getPermission().getCode()).forEach(changed::add);
        selected.stream().filter(p -> !previousIds.contains(p.getId())).map(Permission::getCode).forEach(changed::add);
        policy.requireDelegablePermissions(changed);
        Set<String> before = previous.stream().map(rp -> rp.getPermission().getCode()).collect(Collectors.toSet());
        // Chỉ xóa/thêm phần chênh lệch, tránh delete rồi insert lại cùng composite key.
        links.deleteAll(previous.stream()
                .filter(rp -> !request.getPermissionIds().contains(rp.getPermission().getId())).toList());
        links.saveAll(selected.stream().filter(p -> !previousIds.contains(p.getId()))
                .map(p -> RolePermission.builder().id(new RolePermissionId(id, p.getId()))
                        .role(role).permission(p).build()).toList());
        links.flush();
        // Thay đổi join table cũng phải làm version của role tăng.
        role.setUpdatedAt(Instant.now());
        roles.flush();
        Set<String> after = selected.stream().map(Permission::getCode).collect(Collectors.toSet());
        audit.write("ROLE_PERMISSIONS_CHANGED", "ROLE", id,
                Map.of("permissions", before), Map.of("permissions", after), request.getReason());
        return response(role);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('ROLE_MANAGE')")
    public void delete(Long id, VersionedReasonRequest request) {
        policy.lockAdministration();
        Role role = requireRole(id);
        ChangeTracker.requireVersion(role.getVersion(), request.getVersion());
        if (RbacPolicy.SYSTEM_ROLES.contains(role.getCode()) || users.existsByRoles_Id(id)) {
            throw new BusinessException(409, "Không xóa role hệ thống hoặc role đang được sử dụng");
        }
        policy.requireDelegableRoles(List.of(role));
        Map<String, Object> before = snapshot(role);
        links.deleteAll(links.findByRole_Id(id));
        links.flush();
        roles.delete(role);
        roles.flush();
        audit.write("ROLE_DELETED", "ROLE", id, before, Map.of(), request.getReason());
    }

    private Role requireRole(Long id) {
        return roles.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("Role không tồn tại"));
    }

    private AdminRoleResponse response(Role role) {
        Set<String> codes = links.findByRole_Id(role.getId()).stream()
                .map(rp -> rp.getPermission().getCode()).collect(Collectors.toSet());
        return new AdminRoleResponse(role.getId(), role.getCode(), role.getName(),
                role.getDescription(), codes, role.getVersion());
    }

    private Map<String, Object> snapshot(Role role) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", role.getCode());
        result.put("name", role.getName());
        result.put("description", role.getDescription());
        return result;
    }
}
