package com.vn.datphong.service;

import com.vn.datphong.entity.OutboxEvent;


public interface OutboxService {

    OutboxEvent publish(String eventType, String aggregateType, Long aggregateId, String payload);
}
