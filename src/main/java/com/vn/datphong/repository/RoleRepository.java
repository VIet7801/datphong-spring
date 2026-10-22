package com.vn.datphong.repository;

import com.vn.datphong.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByCode(String code);

    Optional<Role> findByCode(String code);

    Slice<Role> findAllBy(Pageable pageable);

    /** Serialize thay đổi quyền/trạng thái để bảo vệ admin cuối cùng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.code = :code")
    Optional<Role> findByCodeForUpdate(@Param("code") String code);
}
