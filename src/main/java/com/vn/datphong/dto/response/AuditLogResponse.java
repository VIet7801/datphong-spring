package com.vn.datphong.dto.response;

import com.vn.datphong.enums.ActorType;
import com.vn.datphong.enums.AuditOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID eventId,
        Long actorId,
        String actorUsername,
        ActorType actorType,
        String action,
        String entityType,
        Long entityId,
        String fromStatus,
        String toStatus,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        String reason,
        String requestId,
        String correlationId,
        Instant occurredAt,
        AuditOutcome outcome
) {
}