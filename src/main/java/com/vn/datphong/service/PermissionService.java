package com.vn.datphong.service;

import com.vn.datphong.dto.request.*;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.entity.Permission;
import com.vn.datphong.exception.*;
import com.vn.datphong.repository.*;
import com.vn.datphong.security.Permissions;
import com.vn.datphong.service.support.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PermissionService {
    private final PermissionRepository permissions;
    private final RolePermissionRepository links;
    private final RbacPolicy policy;
    private final AuditLogWriter audit;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_READ')")
    public SliceResponse<PermissionResponse> search(int page, int size) {
        return SliceResponse.from(permissions.findAllBy(PageRequest.of(page, size, Sort.by("id"))), this::response);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_READ')")
    public PermissionResponse get(Long id) {
        return response(requirePermission(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_MANAGE')")
    public PermissionResponse create(PermissionCreateRequest request) {
        policy.lockAdministration();
        if (permissions.existsByCode(request.getCode())) throw new BusinessException(409, "Mã permission đã tồn tại");
        Permission permission = permissions.saveAndFlush(Permission.builder().code(request.getCode())
                .name(request.getName()).description(request.getDescription()).build());
        audit.write("PERMISSION_CREATED", "PERMISSION", permission.getId(),
                Map.of(), snapshot(permission), request.getReason());
        return response(permission);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_MANAGE')")
    public PermissionResponse update(Long id, PermissionUpdateRequest request) {
        policy.lockAdministration();
        Permission permission = requirePermission(id);
        ChangeTracker.requireVersion(permission.getVersion(), request.getVersion());
        if (Objects.equals(permission.getName(), request.getName())
                && Objects.equals(permission.getDescription(), request.getDescription())) return response(permission);
        Map<String, Object> before = snapshot(permission);
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permissions.flush();
        audit.write("PERMISSION_UPDATED", "PERMISSION", id, before, snapshot(permission), request.getReason());
        return response(permission);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_MANAGE')")
    public void delete(Long id, VersionedReasonRequest request) {
        policy.lockAdministration();
        Permission permission = requirePermission(id);
        ChangeTracker.requireVersion(permission.getVersion(), request.getVersion());
        if (Permissions.BUILT_IN.contains(permission.getCode()) || links.existsByPermission_Id(id)) {
            throw new BusinessException(409, "Không xóa permission hệ thống hoặc permission đang được sử dụng");
        }
        Map<String, Object> before = snapshot(permission);
        permissions.delete(permission);
        permissions.flush();
        audit.write("PERMISSION_DELETED", "PERMISSION", id, before, Map.of(), request.getReason());
    }

    private Permission requirePermission(Long id) {
        return permissions.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("Permission không tồn tại"));
    }

    private PermissionResponse response(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getName(),
                permission.getDescription(), permission.getVersion());
    }

    private Map<String, Object> snapshot(Permission permission) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", permission.getCode());
        result.put("name", permission.getName());
        result.put("description", permission.getDescription());
        return result;
    }
}
