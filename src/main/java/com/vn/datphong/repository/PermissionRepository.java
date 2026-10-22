package com.vn.datphong.repository;

import com.vn.datphong.entity.Permission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByCode(String code);
    Slice<Permission> findAllBy(Pageable pageable);
}
