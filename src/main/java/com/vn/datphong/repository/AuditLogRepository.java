package com.vn.datphong.repository;

import com.vn.datphong.entity.AuditLog;
import com.vn.datphong.enums.AuditOutcome;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @EntityGraph(attributePaths = "actor")
    //JPQL
    @Query("""
            select a
            from AuditLog a
            left join a.actor actor
            where (:actorId is null or actor.id = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:action is null or a.action = :action)
              and (:outcome is null or a.outcome = :outcome)
              and a.occurredAt >= :from
              and a.occurredAt < :to
            """)
/**
 * A slice of data that indicates whether there's a next or previous slice available. Allows to obtain a
 * {Pageable} to request a previous or next { Slice}. */
    Slice<AuditLog> search(
            @Param("actorId") Long actorId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("action") String action,
            @Param("outcome") AuditOutcome outcome,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}