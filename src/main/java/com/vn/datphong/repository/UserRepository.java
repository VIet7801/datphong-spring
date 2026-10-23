package com.vn.datphong.repository;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
// Spring Data tạo implementation; Hibernate thực hiện SQL và ánh xạ bản ghi thành entity User.
// Repository không tạo JSON và không phải nơi quyết định quyền gọi API.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("select u from User u where lower(trim(u.email)) = :email")
    Optional<User> findByNormalizedEmail(@Param("email") String email);

    @Query("select u.id from User u where lower(trim(u.email)) = :email")
    Optional<Long> findIdByNormalizedEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsernameForUpdate(@Param("username") String username);

    boolean existsByUsername(String username);

    boolean existsByIdAndUsername(Long id, String username);

    // Email được chuẩn hóa trim/lowercase khi đăng ký; IgnoreCase để vẫn khớp dữ liệu cũ chưa chuẩn hóa.
    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRoles_Id(Long id);

    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%')))
            """)
    Slice<User> search(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    @Query("""
            select count(distinct u.id) from User u join u.roles r
            where r.code = 'ROLE_ADMIN' and u.active = true and u.status = :status
              and u.emailVerifiedAt is not null
            """)
    long countActiveAdmins(@Param("status") UserStatus status);
}
