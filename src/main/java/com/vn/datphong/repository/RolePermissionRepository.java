package com.vn.datphong.repository;

import com.vn.datphong.entity.RolePermission;
import com.vn.datphong.entity.RolePermissionId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findByRole_Id(Long roleId);

    boolean existsByPermission_Id(Long permissionId);

    @Query("""
            select distinct rp.permission.code from RolePermission rp
            where rp.role.id in (select r.id from User u join u.roles r where u.id = :userId)
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
