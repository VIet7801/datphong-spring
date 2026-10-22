package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.AuditLogResponse;
import com.vn.datphong.dto.response.AuditLogSearchResponse;
import com.vn.datphong.entity.AuditLog;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.mapper.AuditLogMapper;
import com.vn.datphong.repository.AuditLogRepository;
import com.vn.datphong.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
//kiểm tra quyền trước khi chạy thân hàm, không phải "ROLE_ADMIN" = cook
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository repository;

    @Override
    @Transactional(readOnly = true)
    public AuditLogSearchResponse search(AuditLogSearchRequest request) {
        Instant from = request.getFrom().toInstant();
        Instant to = request.getTo().toInstant();

        if (!from.isBefore(to)) {
            throw new BusinessException(400, "from phải trước to");
        }

        if (Duration.between(from, to).compareTo(Duration.ofDays(31)) > 0) {
            throw new BusinessException(400, "Mỗi lần tìm tối đa 31 ngày");
        }

        if (request.getEntityId() != null && request.getEntityType() == null) {
            throw new BusinessException(
                    400, "Khi có entityId phải truyền thêm entityType"
            );
        }
        //Thứ tự occurredAt giảm dần đưa sự kiện mới lên trước
        // id giảm dần xác định thứ tự khi nhiều sự kiện cùng thời điểm
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("id")
                )
        );

        Slice<AuditLog> result = repository.search(
                request.getActorId(),
                request.getEntityType(),
                request.getEntityId(),
                request.getAction(),
                request.getOutcome(),
                from,
                to,
                pageable
        );

        List<AuditLogResponse> items = result.getContent()
                .stream()
                .map(AuditLogMapper::toResponse)
                .toList();

        return new AuditLogSearchResponse(
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                items
        );
    }
}