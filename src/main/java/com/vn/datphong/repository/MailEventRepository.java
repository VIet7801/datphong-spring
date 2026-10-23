package com.vn.datphong.repository;

import com.vn.datphong.entity.MailEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MailEventRepository extends JpaRepository<MailEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MailEvent m where m.id = :id")
    Optional<MailEvent> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "templateVersion")
    @Query("""
            select m from MailEvent m
            where m.eventType = com.vn.datphong.enums.MailEventType.EMAIL_VERIFICATION
              and ((m.status in (com.vn.datphong.enums.MailStatus.QUEUED, com.vn.datphong.enums.MailStatus.RETRY)
                    and m.dueAt <= :now)
                or (m.status = com.vn.datphong.enums.MailStatus.SENDING and m.leaseUntil <= :now))
            order by m.id
            """)
    List<MailEvent> findReadyForUpdate(@Param("now") Instant now, Pageable page);

    List<MailEvent> findByRecipient_IdAndEventTypeAndStatusIn(Long userId,
            com.vn.datphong.enums.MailEventType type, java.util.Collection<com.vn.datphong.enums.MailStatus> statuses);
}
