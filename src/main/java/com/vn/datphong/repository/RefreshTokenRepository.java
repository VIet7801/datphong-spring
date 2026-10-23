package com.vn.datphong.repository;

import com.vn.datphong.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Projection trước khi khóa User: không nạp entity token cũ vào persistence context.
    @Query("select t.user.id from RefreshToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String hash);

    boolean existsByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(UUID familyId, Instant now);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") Instant now);
}
