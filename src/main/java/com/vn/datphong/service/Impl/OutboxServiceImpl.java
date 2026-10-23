package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.OutboxEvent;
import com.vn.datphong.enums.OutboxStatus;
import com.vn.datphong.repository.OutboxEventRepository;
import com.vn.datphong.service.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;


@Service
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository repository;
    private final Clock clock;

    public OutboxServiceImpl(OutboxEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publish(String eventType, String aggregateType, Long aggregateId, String payload) {
        OutboxEvent e = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload == null ? "" : payload)
                .status(OutboxStatus.PENDING)
                .availableAt(LocalDateTime.now(clock))
                .build();
        return repository.save(e);
    }
}
