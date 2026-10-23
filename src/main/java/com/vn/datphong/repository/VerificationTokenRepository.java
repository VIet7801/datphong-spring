package com.vn.datphong.repository;

import com.vn.datphong.entity.VerificationToken;
import com.vn.datphong.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Caller khóa hàng User trước khi tạo/đọc/sử dụng token. */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findFirstByUser_IdAndPurposeOrderByIdDesc(Long userId, VerificationPurpose purpose);
}
