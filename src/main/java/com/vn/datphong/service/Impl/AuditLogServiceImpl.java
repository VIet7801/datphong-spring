package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.AuditLogResponse;
import com.vn.datphong.dto.response.AuditLogSearchResponse;
import com.vn.datphong.entity.AuditLog;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.mapper.AuditLogMapper;
import com.vn.datphong.repository.AuditLogRepository;
import com.vn.datphong.service.AuditLogService;
import com.vn.datphong.service.AuditLogWriter;
import com.vn.datphong.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// Chính sách được chốt theo yêu cầu: chỉ ADMIN, đồng thời phải có AUDIT_READ.
@PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('AUDIT_READ')")
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository repository;
    private final AuditLogWriter audit;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public AuditLogSearchResponse search(AuditLogSearchRequest request) {
        if (request.getFrom() == null || request.getTo() == null) {
            throw new BusinessException(400, "Cần truyền from và to");
        }
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

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("actorId", request.getActorId());
        filters.put("entityType", request.getEntityType());
        filters.put("entityId", request.getEntityId());
        filters.put("action", request.getAction());
        filters.put("outcome", request.getOutcome());
        filters.put("from", from.toString());
        filters.put("to", to.toString());
        filters.put("page", request.getPage());
        filters.put("size", request.getSize());
        // Ghi việc truy cập dữ liệu nhạy cảm; không sao chép nội dung audit vào event mới.
        audit.write("AUDIT_LOG_SEARCHED", "AUDIT_LOG_QUERY", currentUser.require().getId(),
                Map.of(), filters, "ADMIN tra cứu Audit Log");

        return new AuditLogSearchResponse(
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                items
        );
    }
}
