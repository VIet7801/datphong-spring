package com.vn.datphong.repository;

import com.vn.datphong.entity.FakeBankWebhookEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface FakeBankWebhookEventRepository extends JpaRepository<FakeBankWebhookEvent, Long> {
    @Query("select e.id from FakeBankWebhookEvent e where e.status = 'PENDING' and e.availableAt <= :now order by e.id")
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);
}