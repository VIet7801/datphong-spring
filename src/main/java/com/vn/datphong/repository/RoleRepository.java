package com.vn.datphong.repository;

import com.vn.datphong.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByCode(String Code);
    @Query(value = "select * from roles",nativeQuery = true)
    List<Role> findAll();

}
