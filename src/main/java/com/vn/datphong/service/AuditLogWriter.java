package com.vn.datphong.service;

import com.vn.datphong.entity.AuditLog;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.ActorType;
import com.vn.datphong.enums.AuditOutcome;
import com.vn.datphong.repository.AuditLogRepository;
import com.vn.datphong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogWriter {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    //yêu cầu caller đã có transaction
    @Transactional(propagation = Propagation.MANDATORY)
    public void userUpdated(
            Long targetUserId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String operationId
    ) {
        save("USER_UPDATED", "USER", targetUserId, beforeState, afterState,
                "Cập nhật thông tin hồ sơ", operationId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String action, String entityType, Long entityId,
                      Map<String, Object> beforeState, Map<String, Object> afterState,
                      String reason) {
        save(action, entityType, entityId, beforeState, afterState, reason, UUID.randomUUID().toString());
    }

    private void save(String action, String entityType, Long entityId,
                      Map<String, Object> beforeState, Map<String, Object> afterState,
                      String reason, String operationId) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Cần đăng nhập để xác định người thực hiện"
            );
        }

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "Không tìm thấy người thực hiện"
                        )
                );

        AuditLog log = AuditLog.builder()
                .eventId(UUID.randomUUID())
                .actor(actor)
                .actorType(ActorType.USER)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(beforeState.get("status") == null ? null : beforeState.get("status").toString())
                .toStatus(afterState.get("status") == null ? null : afterState.get("status").toString())
                .beforeState(new LinkedHashMap<>(beforeState))
                .afterState(new LinkedHashMap<>(afterState))
                .reason(reason)
                .requestId(operationId)
                .correlationId(operationId)
                .occurredAt(Instant.now())
                .outcome(AuditOutcome.SUCCESS)
                .build();

        auditLogRepository.save(log);
    }
}
