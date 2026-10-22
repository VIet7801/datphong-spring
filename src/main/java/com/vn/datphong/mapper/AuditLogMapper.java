package com.vn.datphong.mapper;

import com.vn.datphong.dto.response.AuditLogResponse;
import com.vn.datphong.entity.AuditLog;
import com.vn.datphong.entity.User;

public final class AuditLogMapper {
    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog log) {
        User actor = log.getActor();

        return new AuditLogResponse(
                log.getId(),
                log.getEventId(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getUsername(),
                log.getActorType(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getBeforeState(),
                log.getAfterState(),
                log.getReason(),
                log.getRequestId(),
                log.getCorrelationId(),
                log.getOccurredAt(),
                log.getOutcome()
        );
    }
}