package com.vn.datphong.repository;

import com.vn.datphong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// Spring Data tạo implementation; Hibernate thực hiện SQL và ánh xạ bản ghi thành entity User.
// Repository không tạo JSON và không phải nơi quyết định quyền gọi API.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByIdAndUsername(Long id, String username);

    // Email được chuẩn hóa trim/lowercase khi đăng ký; IgnoreCase để vẫn khớp dữ liệu cũ chưa chuẩn hóa.
    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

}
