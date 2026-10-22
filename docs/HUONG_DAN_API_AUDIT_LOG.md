**Bài thực hành: ghi và tìm kiếm Audit Log nghiệp vụ**

Bản hướng dẫn này được chỉnh theo yêu cầu chỉ làm Audit Log nghiệp vụ. Ta dùng Entity AuditLog và bảng audit_logs đã có trong dự án. Bài học tập trung vào việc biết ai đã làm gì, tác động tới đối tượng nào, khi nào và thay đổi dữ liệu ra sao.

Đây là tài liệu để bạn tự thực hành trong IntelliJ. Các file Java trong code block là mẫu để áp dụng, chưa được tự động cài đặt vào source ứng dụng. Việc sửa tài liệu không thực thi SQL trên database.

Kiểm chứng: 9 file Java mẫu cùng một bản UserServiceImpl ghép theo hướng dẫn đã biên dịch thành công với Java 17 và dependency của dự án trong thư mục kiểm tra riêng dưới target. Đã kiểm tra cú pháp các ví dụ JSON và cấu trúc code block. Chưa chạy SQL hoặc kiểm thử API mẫu trên database thật.

**1. Chốt chức năng bằng một ví dụ**

Giả sử ADMIN có ID 12 sửa tên của User 34 từ “An” thành “Nguyễn An”.

Hệ thống cần lưu một sự kiện:

~~~text
Người thực hiện:    User 12
Hành động:         USER_UPDATED
Loại đối tượng:    USER
ID đối tượng:      34
Trước thay đổi:    fullName = An
Sau thay đổi:      fullName = Nguyễn An
Thời điểm:         2026-10-21T02:00:00Z
Kết quả:           SUCCESS
~~~

Sau đó ADMIN có thể tìm:

- User 12 đã thực hiện những hành động nào?
- Ai đã tác động tới hồ sơ User 34?
- Ai đã sửa hồ sơ User 34 trong khoảng 08:00 tới 10:00?
- Hồ sơ đó đã thay đổi những trường nào?

Trong bài này, USER_UPDATED chỉ được ghi khi ít nhất một trường được cập nhật thực sự có giá trị khác trước. Gửi lại nguyên dữ liệu cũ không tạo thêm một sự kiện thay đổi.

**2. Phân biệt người thực hiện và đối tượng bị tác động**

| Tên | Ví dụ | Ý nghĩa |
|---|---|---|
| Người đang tìm kiếm | ADMIN ID 1 | Người có token để gọi API xem lịch sử |
| actor | User ID 12 | Người đã thực hiện hành động |
| entityType | USER | Loại đối tượng bị tác động |
| entityId | 34 | ID đối tượng bị tác động |
| action | USER_UPDATED | Mã hành động nghiệp vụ |

actorId và entityId có thể khác nhau. Cũng có thể bằng nhau nếu người dùng tự sửa hồ sơ.

Muốn tìm “User 12 đã làm gì”, lọc actorId=12.

Muốn tìm “ai đã sửa User 34”, lọc entityType=USER, entityId=34 và action=USER_UPDATED. Kết quả của từng dòng cho biết actor.

**3. Đọc Entity có sẵn trước khi tạo file mới**

Mở src/main/java/com/vn/datphong/entity/AuditLog.java.

| Field Java | Cột database | Ý nghĩa |
|---|---|---|
| id | id | Khóa chính của dòng audit |
| eventId | event_id | UUID nhận diện sự kiện |
| actor | actor_id | Quan hệ tới User thực hiện |
| actorType | actor_type | USER, SYSTEM hoặc PROVIDER |
| action | action | Mã hành động, ví dụ USER_UPDATED |
| entityType | entity_type | Loại đối tượng, ví dụ USER hoặc BOOKING |
| entityId | entity_id | ID đối tượng bị tác động |
| fromStatus / toStatus | from_status / to_status | Trạng thái trước/sau nếu nghiệp vụ có trạng thái |
| beforeState / afterState | before_state / after_state | Dữ liệu trước/sau dạng JSON |
| reason | reason | Lý do hoặc diễn giải |
| requestId / correlationId | request_id / correlation_id | Mã liên kết một lần xử lý/chuỗi xử lý |
| occurredAt | occurred_at | Thời điểm sự kiện |
| outcome | outcome | ATTEMPT, SUCCESS hoặc FAILURE |
| createdAt | created_at | Thời điểm lưu dòng audit |

Không tạo thêm Entity hoặc bảng cho bài này. Bảng audit_logs đã nằm trong các script khởi tạo schema của dự án.

Enum ATTEMPT/FAILURE tồn tại không có nghĩa hệ thống tự ghi các loại sự kiện đó. Phần ghi trong bài này chỉ minh họa SUCCESS của một thay đổi đã commit.

**4. Có hai luồng cần xây dựng**

~~~text
Luồng ghi:
API cập nhật hồ sơ
  → UserServiceImpl.update
  → đọc dữ liệu cũ và thực hiện thay đổi
  → AuditLogWriter ghi sự kiện
  → commit cập nhật User và audit trong cùng transaction

Luồng đọc:
ADMIN gửi điều kiện tìm kiếm
  → AuditLogController
  → AuditLogService
  → AuditLogRepository đọc audit_logs
  → Mapper tạo DTO
  → trả danh sách JSON có phân trang
~~~

Tạo API đọc sẽ giúp xem dữ liệu đã tồn tại. Để những hành động mới xuất hiện trong kết quả, phải bổ sung phần ghi vào Service thực hiện nghiệp vụ.

**5. Chốt hợp đồng API tìm kiếm**

Endpoint dùng trong bài:

~~~http
GET /api/admin/audit-logs
Authorization: Bearer <token-admin>
~~~

| Tham số | Bắt buộc | Ý nghĩa |
|---|---|---|
| actorId | Không | Người thực hiện |
| entityType | Không | Loại đối tượng bị tác động, viết hoa |
| entityId | Không | ID đối tượng; khi truyền phải có entityType |
| action | Không | Mã hành động, viết hoa |
| outcome | Không | SUCCESS, FAILURE hoặc ATTEMPT |
| from | Có | Thời điểm bắt đầu có timezone |
| to | Có | Thời điểm kết thúc có timezone |
| page | Không | Trang bắt đầu từ 0, mặc định 0 |
| size | Không | Số dòng mỗi trang, mặc định 20, tối đa 100 |

from/to bắt buộc để giới hạn khoảng tìm. Bài này cho phép tìm tối đa 31 ngày một lần.

Khoảng thời gian là [from,to):

~~~text
occurredAt >= from
occurredAt < to
~~~

08:00 được tính; 10:00 không được tính trong khoảng 08:00–10:00. Khoảng 10:00–12:00 sẽ nhận sự kiện lúc 10:00.

Ví dụ:

~~~http
GET /api/admin/audit-logs?actorId=12&from=2026-10-21T01:00:00Z&to=2026-10-21T03:00:00Z
~~~

Đây là lịch sử của actor 12 từ 08:00 tới trước 10:00 ở UTC+07:00.

~~~http
GET /api/admin/audit-logs?entityType=USER&entityId=34&action=USER_UPDATED&from=2026-10-21T01:00:00Z&to=2026-10-21T03:00:00Z
~~~

Đây là các lần sửa hồ sơ User 34 trong cùng khoảng thời gian. Có thể thêm actorId để kết hợp cả hai điều kiện.

**6. Chuẩn bị các file theo package**

Trong IntelliJ, mở src/main/java/com/vn/datphong. Nhấp phải package tương ứng → New → Java Class → nhập tên lớp.

~~~text
dto/request/AuditLogSearchRequest.java
dto/response/AuditLogResponse.java
dto/response/AuditLogSearchResponse.java
repository/AuditLogRepository.java
mapper/AuditLogMapper.java
service/AuditLogService.java
service/Impl/AuditLogServiceImpl.java
controller/AuditLogController.java
service/AuditLogWriter.java
~~~

Sau đó ghép một field và thay phương thức update trong UserServiceImpl theo bước 16. Entity AuditLog, UserRepository, ApiResponse và GlobalExceptionHandler dùng lại của dự án.

**7. Tạo Request DTO**

~~~java
// File: src/main/java/com/vn/datphong/dto/request/AuditLogSearchRequest.java
package com.vn.datphong.dto.request;

import com.vn.datphong.enums.AuditOutcome;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;

@Getter
@Setter
public class AuditLogSearchRequest {
    @Positive(message = "actorId phải lớn hơn 0")
    private Long actorId;

    @Pattern(
            regexp = "[A-Z][A-Z0-9_]{0,49}",
            message = "entityType phải là mã viết hoa, tối đa 50 ký tự"
    )
    private String entityType;

    @Positive(message = "entityId phải lớn hơn 0")
    private Long entityId;

    @Pattern(
            regexp = "[A-Z][A-Z0-9_]{0,99}",
            message = "action phải là mã viết hoa, tối đa 100 ký tự"
    )
    private String action;

    private AuditOutcome outcome;

    @NotNull(message = "from không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;

    @NotNull(message = "to không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime to;

    @Min(value = 0, message = "page phải từ 0 trở lên")
    private int page = 0;

    @Min(value = 1, message = "size phải từ 1 đến 100")
    @Max(value = 100, message = "size phải từ 1 đến 100")
    private int size = 20;
}
~~~

Positive và Pattern cho phép null. Vì vậy bỏ một bộ lọc nghĩa là không hạn chế theo trường đó. Nếu không muốn dùng action trong Postman, bỏ chọn tham số; đừng gửi action rỗng.

outcome dùng enum để chỉ nhận các giá trị mà dự án định nghĩa.

OffsetDateTime nhận thời gian kèm offset, ví dụ 2026-10-21T08:00:00+07:00. Service chuyển nó thành Instant để so sánh.

**8. Tạo DTO biểu diễn một dòng audit**

~~~java
// File: src/main/java/com/vn/datphong/dto/response/AuditLogResponse.java
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
~~~

DTO chỉ lấy những trường cần công bố. Không trả nguyên Entity User để tránh đưa cả password/secret hoặc các quan hệ khác vào response.

Actor có thể null với sự kiện SYSTEM/PROVIDER. Response vẫn giữ actorType để người đọc hiểu nguồn phát sinh.

**9. Tạo DTO chứa danh sách phân trang**

~~~java
// File: src/main/java/com/vn/datphong/dto/response/AuditLogSearchResponse.java
package com.vn.datphong.dto.response;

import java.util.List;

public record AuditLogSearchResponse(
        int page,
        int size,
        boolean hasNext,
        List<AuditLogResponse> items
) {
}
~~~

Ta dùng Slice, lấy một phần dữ liệu và biết có trang tiếp theo hay không. Response không cần tổng số bản ghi.

Ví dụ page=0, size=20, hasNext=true: đang trả trang đầu và vẫn còn dữ liệu phía sau.

**10. Tạo Repository để tìm kiếm**

~~~java
// File: src/main/java/com/vn/datphong/repository/AuditLogRepository.java
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
~~~

Đây là JPQL, dùng tên Entity và field Java. Trong SQL tên cột là occurred_at; trong JPQL tên field là a.occurredAt.

Đọc điều kiện theo từng dòng:

~~~text
actorId có giá trị      → chỉ lấy sự kiện do người đó thực hiện
entityType có giá trị   → chỉ lấy đúng loại đối tượng
entityId có giá trị     → chỉ lấy đúng ID đối tượng
action có giá trị       → chỉ lấy đúng mã hành động
outcome có giá trị      → chỉ lấy đúng kết quả
from/to                 → giới hạn thời gian
~~~

LEFT JOIN giữ được sự kiện có actor null khi không lọc actorId. Dùng INNER JOIN sẽ làm mất những dòng SYSTEM/PROVIDER đó.

EntityGraph yêu cầu tải actor để Mapper đọc id/username. Việc mapping còn nằm trong transaction đọc của Service vì cấu hình dự án đang tắt open-in-view.

Slice lấy dữ liệu đủ để xác định hasNext, không cần thêm truy vấn COUNT tổng số dòng.

**11. Tạo Mapper chuyển Entity thành DTO**

~~~java
// File: src/main/java/com/vn/datphong/mapper/AuditLogMapper.java
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
~~~

Phần actor == null tránh lỗi khi sự kiện do hệ thống thực hiện.

Với Entity hiện tại, actorUsername được lấy từ User tại lúc đọc. Nó chưa phải snapshot username tại lúc phát sinh sự kiện. actorId là thông tin định danh cần dùng để truy vết.

**12. Tạo Service interface**

~~~java
// File: src/main/java/com/vn/datphong/service/AuditLogService.java
package com.vn.datphong.service;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.AuditLogSearchResponse;

public interface AuditLogService {
    AuditLogSearchResponse search(AuditLogSearchRequest request);
}
~~~

**13. Viết Service implementation**

~~~java
// File: src/main/java/com/vn/datphong/service/Impl/AuditLogServiceImpl.java
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
~~~

PreAuthorize kiểm tra quyền trước khi chạy thân hàm. Token hợp lệ của người dùng thường vẫn bị từ chối nếu không có ROLE_ADMIN.

entityId cần đi kèm entityType vì USER có ID 34 và BOOKING có ID 34 là hai đối tượng khác nhau.

Thứ tự occurredAt giảm dần đưa sự kiện mới lên trước; id giảm dần xác định thứ tự khi nhiều sự kiện cùng thời điểm.

Không tìm thấy dữ liệu thì trả items rỗng, hasNext=false, HTTP 200.

**14. Tạo Controller**

~~~java
// File: src/main/java/com/vn/datphong/controller/AuditLogController.java
package com.vn.datphong.controller;

import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.AuditLogSearchResponse;
import com.vn.datphong.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogSearchResponse>> search(
            @Valid @ModelAttribute AuditLogSearchRequest request
    ) {
        AuditLogSearchResponse result = auditLogService.search(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
~~~

ModelAttribute đưa query parameters vào Request DTO. Valid kiểm tra các ràng buộc. Theo [tài liệu Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html), binding/validation lỗi trong trường hợp này được báo bằng MethodArgumentNotValidException khi không có BindingResult đứng sau. GlobalExceptionHandler hiện có của dự án đã trả HTTP 400 cho loại lỗi đó.

Giữ EnableMethodSecurity đang có trong SecurityConfig. Với anyRequest().authenticated() và PreAuthorize ở Service như trên, API đọc audit đã yêu cầu đăng nhập và quyền ADMIN. Bài học không cần thêm Filter hoặc dependency vào SecurityFilterChain.

Tới đây, bạn đã có API tìm kiếm trên các sự kiện được lưu trong audit_logs. Bước tiếp theo là ghi thêm lịch sử khi nghiệp vụ thực sự thay đổi dữ liệu.

**15. Tạo Writer ghi audit cùng transaction nghiệp vụ**

~~~java
// File: src/main/java/com/vn/datphong/service/AuditLogWriter.java
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void userUpdated(
            Long targetUserId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String operationId
    ) {
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
                .action("USER_UPDATED")
                .entityType("USER")
                .entityId(targetUserId)
                .beforeState(new LinkedHashMap<>(beforeState))
                .afterState(new LinkedHashMap<>(afterState))
                .reason("Cập nhật thông tin hồ sơ")
                .requestId(operationId)
                .correlationId(operationId)
                .occurredAt(Instant.now())
                .outcome(AuditOutcome.SUCCESS)
                .build();

        auditLogRepository.save(log);
    }
}
~~~

Các quyết định cần hiểu:

- Actor được tìm từ SecurityContext đã xác thực, không lấy actorId do client gửi.
- targetUserId lấy từ User mà Service đã kiểm tra quyền và đang cập nhật.
- USER_UPDATED và USER là mã do code nghiệp vụ quy định.
- eventId nhận diện một sự kiện.
- operationId được Service sinh một lần để liên kết các sự kiện trong một lần xử lý.
- Bài này dùng operationId cho hai cột bắt buộc requestId/correlationId. Nó là mã xử lý nghiệp vụ nội bộ; chưa đại diện cho một mã request bên ngoài được truyền xuyên hệ thống.
- MANDATORY yêu cầu caller đã có transaction. Writer tham gia transaction đó.

Điều này bảo đảm: cập nhật User và audit SUCCESS cùng commit hoặc cùng rollback. Không bắt lỗi ghi audit rồi bỏ qua trong luồng này, vì như vậy có thể sửa dữ liệu thành công nhưng mất lịch sử.

Không đặt quyền chỉ ADMIN lên Writer: người dùng được phép tự sửa hồ sơ cũng cần được ghi lịch sử. Quyền sửa hồ sơ do UserServiceImpl kiểm tra, quyền xem audit do AuditLogServiceImpl kiểm tra.

Phương thức này không tạo endpoint cho client tự gửi một dòng audit.

**16. Ghép phần ghi vào UserServiceImpl.update**

Mở src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java.

Thêm các import nếu chưa có:

~~~java
import com.vn.datphong.service.AuditLogWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
~~~

Trong thân lớp, thêm dependency:

~~~java
@Autowired
private AuditLogWriter auditLogWriter;
~~~

Giữ các phương thức khác. Thay phương thức update hiện có và thêm hai phương thức private dưới đây trong cùng lớp:

~~~java
// Replace in: src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java
@Override
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @userAuthorization.isOwner(#id, authentication)")
@Transactional(propagation = Propagation.REQUIRED)
public UsersResponse update(Long id, UsersResquest req) {
    User updated = userRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundExcept("Id: " + id + " không tồn tại")
            );

    Map<String, Object> beforeState = new LinkedHashMap<>();
    Map<String, Object> afterState = new LinkedHashMap<>();

    setAndTrack(
            "fullName", updated.getFullName(), req.getFullName(),
            updated::setFullName, beforeState, afterState
    );
    setAndTrack(
            "email", updated.getEmail(), req.getEmail(),
            updated::setEmail, beforeState, afterState
    );
    setAndTrack(
            "phone", updated.getPhone(), req.getPhone(),
            updated::setPhone, beforeState, afterState
    );
    setAndTrack(
            "gender", updated.getGender(), req.getGender(),
            updated::setGender, beforeState, afterState
    );
    setAndTrack(
            "avatar", updated.getAvatar(), req.getAvatar(),
            updated::setAvatar, beforeState, afterState
    );

    if (!beforeState.isEmpty()) {
        String operationId = UUID.randomUUID().toString();
        auditLogWriter.userUpdated(
                updated.getId(), beforeState, afterState, operationId
        );
    }

    return UserMapper.toResponse(updated);
}

private <T> void setAndTrack(
        String field,
        T oldValue,
        T newValue,
        Consumer<T> setter,
        Map<String, Object> beforeState,
        Map<String, Object> afterState
) {
    if (newValue == null || Objects.equals(oldValue, newValue)) {
        return;
    }

    beforeState.put(field, auditValue(field, oldValue));
    afterState.put(field, auditValue(field, newValue));
    setter.accept(newValue);
}

private Object auditValue(String field, Object value) {
    if (value == null) {
        return null;
    }

    return switch (field) {
        case "email", "phone", "avatar" -> "[REDACTED]";
        default -> value;
    };
}
~~~

Đọc phần này chậm theo từng bước:

1. PreAuthorize kiểm tra ADMIN hoặc chủ hồ sơ.
2. findById tải User cần sửa vào transaction.
3. beforeState/afterState ban đầu rỗng.
4. setAndTrack so sánh giá trị cũ với dữ liệu mới.
5. Nếu khác nhau, ghi giá trị trước/sau rồi gọi setter cập nhật Entity.
6. Nếu có thay đổi, sinh operationId và gọi Writer.
7. Khi phương thức trả về qua transaction proxy, Hibernate commit các thay đổi cùng dòng audit.

updated::setFullName tương đương một hàm nhận tên mới rồi gọi updated.setFullName(tênMới).

Ví dụ chỉ đổi tên:

~~~json
{
  "beforeState": {
    "fullName": "An"
  },
  "afterState": {
    "fullName": "Nguyễn An"
  }
}
~~~

Nếu thay đổi email/phone/avatar, hai map vẫn có tên trường tương ứng để biết trường nào đã đổi, nhưng giá trị được che bằng [REDACTED]. Việc so sánh diễn ra trên giá trị thật trước khi che, nên vẫn phát hiện được thay đổi. Bài này không lưu toàn bộ User, password hoặc secret 2FA vào JSON audit.

Map dùng LinkedHashMap vì trạng thái cũ có thể chứa null. Map.of không chấp nhận giá trị null.

Phần minh họa chỉ xử lý đúng năm trường mà phương thức update hiện tại cập nhật. Những trường như password, roles hoặc status không được cập nhật bởi phương thức này.

**17. Kiểm tra dữ liệu có sẵn trước khi thử API đọc**

Trong Database tool window của IntelliJ, kết nối đúng database đang chạy ứng dụng rồi mở Query Console.

~~~sql
SELECT id, username, full_name
FROM users
ORDER BY id;
~~~

~~~sql
SELECT id, actor_id, actor_type, action, entity_type, entity_id,
       before_state, after_state, outcome, occurred_at
FROM audit_logs
ORDER BY occurred_at DESC, id DESC
LIMIT 20;
~~~

Nếu database đã có dữ liệu demo, bạn có thể thấy BOOKING_STATUS_CHANGED. Đây là sự kiện nghiệp vụ hợp lệ để thử API đọc; không phải tự động đã có USER_UPDATED.

Dự án dùng ddl-auto: validate, nên cần schema audit_logs đã được khởi tạo theo README. Bài này không thêm Entity và không làm thay đổi số lượng Entity.

**18. Thêm dữ liệu mẫu nếu muốn có kết quả dễ đối chiếu**

Fixture bên dưới chỉ giả lập lịch sử trong database local để học truy vấn. Nó không sửa hồ sơ User. Để kiểm tra việc tự ghi audit, làm tiếp bước 21 bằng API cập nhật thật.

Chọn một tài khoản ADMIN test và một User test đã có. Thay hai username trong SQL cho đúng. Nếu SELECT không tìm thấy một trong hai tài khoản, INSERT sẽ không có dòng nào.

~~~sql
WITH selected_actor AS (
    SELECT id
    FROM users
    WHERE username = 'USERNAME_ADMIN_TEST'
), selected_target AS (
    SELECT id
    FROM users
    WHERE username = 'USERNAME_USER_TEST'
), events(
    event_id, actor_type, action, occurred_at, before_state, after_state
) AS (
    VALUES
    ('20000000-0000-4000-8000-000000000001'::uuid,
     'USER', 'USER_UPDATED',
     '2026-10-21T08:00:00+07:00'::timestamptz,
     '{"fullName":"An"}'::jsonb,
     '{"fullName":"Nguyễn An"}'::jsonb),
    ('20000000-0000-4000-8000-000000000002'::uuid,
     'USER', 'USER_UPDATED',
     '2026-10-21T09:00:00+07:00'::timestamptz,
     '{"fullName":"Nguyễn An"}'::jsonb,
     '{"fullName":"Nguyễn Văn An"}'::jsonb),
    ('20000000-0000-4000-8000-000000000003'::uuid,
     'USER', 'USER_UPDATED',
     '2026-10-21T10:00:00+07:00'::timestamptz,
     '{"fullName":"Nguyễn Văn An"}'::jsonb,
     '{"fullName":"Nguyễn Văn An A"}'::jsonb),
    ('20000000-0000-4000-8000-000000000004'::uuid,
     'USER', 'USER_UPDATED',
     '2026-10-21T07:59:59+07:00'::timestamptz,
     '{"fullName":"An A"}'::jsonb,
     '{"fullName":"An"}'::jsonb),
    ('20000000-0000-4000-8000-000000000005'::uuid,
     'SYSTEM', 'USER_STATUS_CHANGED',
     '2026-10-21T08:30:00+07:00'::timestamptz,
     '{"status":"ACTIVE"}'::jsonb,
     '{"status":"LOCKED"}'::jsonb)
)
INSERT INTO audit_logs (
    event_id, actor_id, actor_type,
    action, entity_type, entity_id,
    from_status, to_status, before_state, after_state,
    reason, request_id, correlation_id,
    occurred_at, outcome, created_at
)
SELECT
    e.event_id,
    CASE WHEN e.actor_type = 'USER' THEN a.id ELSE NULL END,
    e.actor_type,
    e.action, 'USER', t.id,
    e.before_state ->> 'status', e.after_state ->> 'status',
    e.before_state, e.after_state,
    'Fixture học API Audit Log',
    e.event_id::text, 'lesson-user-profile',
    e.occurred_at, 'SUCCESS', e.occurred_at
FROM selected_actor a
CROSS JOIN selected_target t
CROSS JOIN events e
ON CONFLICT (event_id) DO NOTHING;
~~~

UUID cố định giúp chạy lại fixture cho cùng các tài khoản không tạo thêm dòng trùng.

Trong khoảng [08:00,10:00):

- Lọc action=USER_UPDATED: hai sự kiện của ADMIN test.
- Bỏ lọc action và lọc đúng User test: có thêm USER_STATUS_CHANGED của SYSTEM.
- Sự kiện lúc 07:59:59 và lúc 10:00 không thuộc khoảng.
- Khi lọc actorId của ADMIN, sự kiện SYSTEM không được chọn.

Các nhận xét trên nói về năm dòng fixture. Nếu đã có sự kiện khác cùng điều kiện, API cũng trả những sự kiện đó.

**19. Chạy ứng dụng và gọi API tìm kiếm bằng Postman**

Áp dụng các file mẫu vào đúng package, reload Maven nếu cần, khởi động dịch vụ phụ thuộc và chạy DatphongApplication theo README.

Đăng nhập ADMIN:

~~~http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
~~~

Body → raw → JSON:

~~~json
{
  "username": "USERNAME_ADMIN_CUA_BAN",
  "password": "MAT_KHAU_ADMIN_CUA_BAN"
}
~~~

Lấy data.accessToken.

Tạo request:

~~~http
GET http://localhost:8080/api/admin/audit-logs
~~~

Authorization → Bearer Token → dán token ADMIN.

Trong Params:

| KEY | VALUE |
|---|---|
| entityType | USER |
| entityId | ID của User test đã chọn |
| action | USER_UPDATED |
| from | 2026-10-21T01:00:00Z |
| to | 2026-10-21T03:00:00Z |
| page | 0 |
| size | 1 |

Body → none → Send.

01:00Z–03:00Z là 08:00–10:00 ở UTC+07:00. Có thể nhập offset +07:00 trong Params. Nếu tự viết URL, mã hóa dấu + thành %2B; dùng Z như ví dụ sẽ tránh nhầm dấu + thành khoảng trắng.

Thử lần lượt các câu hỏi:

~~~text
Ai sửa User này?
    entityType=USER, entityId=<ID>, action=USER_UPDATED

ADMIN test đã làm gì?
    actorId=<ID_ADMIN_TEST>

ADMIN test sửa User này lúc nào?
    actorId=<ID_ADMIN_TEST>, entityType=USER,
    entityId=<ID_USER_TEST>, action=USER_UPDATED
~~~

from/to luôn có trong cả ba trường hợp.

**20. Đọc response**

Ví dụ minh họa với actor 12 và target User 34, khi trang đầu lấy sự kiện fixture lúc 09:00:

~~~json
{
  "success": true,
  "message": "Success",
  "data": {
    "page": 0,
    "size": 1,
    "hasNext": true,
    "items": [
      {
        "id": 102,
        "eventId": "20000000-0000-4000-8000-000000000002",
        "actorId": 12,
        "actorUsername": "admin_test",
        "actorType": "USER",
        "action": "USER_UPDATED",
        "entityType": "USER",
        "entityId": 34,
        "fromStatus": null,
        "toStatus": null,
        "beforeState": {
          "fullName": "Nguyễn An"
        },
        "afterState": {
          "fullName": "Nguyễn Văn An"
        },
        "reason": "Fixture học API Audit Log",
        "requestId": "20000000-0000-4000-8000-000000000002",
        "correlationId": "lesson-user-profile",
        "occurredAt": "2026-10-21T02:00:00Z",
        "outcome": "SUCCESS"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-10-21T04:00:00Z"
}
~~~

id, actorId, entityId, username và timestamp phụ thuộc dữ liệu thật của bạn.

Đổi page=1 để nhận sự kiện fixture lúc 08:00. Với đúng hai sự kiện phù hợp và size=1, trang thứ hai có hasNext=false.

**21. Thử phần tự ghi bằng API sửa hồ sơ thật**

Sau khi ghép Writer vào UserServiceImpl, lấy hồ sơ tài khoản test trước:

~~~http
GET http://localhost:8080/users/detail/34
Authorization: Bearer <token-admin>
~~~

Thay 34 bằng ID tài khoản test của bạn. Ghi lại các giá trị email, phone, username, gender, status và role hiện tại.

API cập nhật đang có là:

~~~http
PUT http://localhost:8080/users/update/34
Authorization: Bearer <token-admin>
Content-Type: application/json
~~~

Controller hiện dùng Valid với UsersResquest chung cho tạo/cập nhật. Vì DTO đó bắt buộc nhiều trường, chỉ gửi fullName sẽ bị validation từ chối trước Service.

Body minh họa cấu trúc đầy đủ:

~~~json
{
  "fullName": "Nguyễn An - Audit test",
  "email": "user.test@example.com",
  "phone": "0912345678",
  "username": "user_test",
  "password": "ValidationOnly123!",
  "gender": true,
  "status": "ACTIVE",
  "roleId": [2]
}
~~~

Trước khi Send, thay email, phone, username, gender, status và roleId bằng dữ liệu hợp lệ của chính tài khoản test. Giữ các trường đang được cập nhật như email/phone/gender bằng giá trị hiện tại nếu chỉ muốn thử thay tên.

roleId lấy từ các id trong data.role của response hồ sơ. password trong ví dụ chỉ để thỏa validation của DTO dùng chung; phương thức update hiện tại không thay password. Nó cũng không thay username, status hoặc roleId. Việc tách DTO cập nhật hồ sơ là một cải tiến riêng, không cần làm để học Audit Log.

Sau khi cập nhật thành công:

1. Gọi API audit với entityType=USER, entityId bằng ID test, action=USER_UPDATED.
2. Chọn from/to bao quanh thời điểm vừa sửa, không dùng khoảng fixture cũ nếu đã khác giờ.
3. Dòng mới phải có actor đúng người sở hữu token, beforeState/afterState đúng phần thay đổi, outcome=SUCCESS.
4. Gửi lại cùng dữ liệu: không phát sinh thêm USER_UPDATED.

Không thêm actorId vào body cập nhật để nhận diện người thực hiện. Writer tự lấy danh tính từ phiên xác thực hiện tại.

**22. Các tình huống cần tự kiểm tra**

| Tình huống | Kết quả cần đạt |
|---|---|
| ADMIN, bộ lọc hợp lệ | HTTP 200 và danh sách phù hợp |
| Chỉ actorId | Lịch sử do đúng người đó thực hiện trong khoảng |
| entityType=USER, entityId=<ID> | Lịch sử tác động tới đúng User |
| Kết hợp actorId và entityId/entityType | Các điều kiện cùng được áp dụng |
| action=USER_UPDATED | Chỉ các sự kiện sửa hồ sơ |
| Không lọc actorId, có sự kiện SYSTEM | Dòng có actor null vẫn xuất hiện |
| Lọc actorId cụ thể | Dòng SYSTEM có actor null không được chọn |
| Sự kiện đúng from | Được chọn |
| Sự kiện đúng to | Không được chọn |
| Thiếu from hoặc to | HTTP 400 |
| from bằng/sau to hoặc khoảng quá 31 ngày | HTTP 400 |
| Có entityId nhưng thiếu entityType | HTTP 400 |
| actorId âm, size=101, outcome sai | HTTP 400 |
| Token user thường gọi API đọc audit | HTTP 403 |
| Không có token hoặc JWT không hợp lệ | HTTP 401 |
| Không có sự kiện phù hợp | HTTP 200, items rỗng, hasNext=false |
| Sửa giá trị thực sự khác | Có thêm USER_UPDATED sau commit |
| Gửi lại nguyên giá trị cũ | Không thêm USER_UPDATED |
| Cập nhật thất bại và transaction rollback | Không giữ một audit SUCCESS của thay đổi đó |

Bài này chỉ ghi những thay đổi mà bạn đã ghép Writer vào. Thêm AuditLog cho booking, thanh toán hoặc nghiệp vụ khác cần gọi Writer tương ứng tại Service đó và quy định action/entityType phù hợp.

**23. Đặt breakpoint để nhìn rõ luồng**

Trong IntelliJ, bấm vào lề trái cạnh dòng code:

1. AuditLogController: dòng gọi auditLogService.search.
2. AuditLogServiceImpl: dòng repository.search.
3. AuditLogMapper: dòng đọc actor và tạo DTO.
4. UserServiceImpl.update: dòng lấy User và dòng gọi auditLogWriter.
5. AuditLogWriter: dòng lấy Authentication và dòng auditLogRepository.save.

Chạy bằng Debug rồi gửi request từ Postman.

Ở luồng đọc, quan sát actorId trong DTO tìm kiếm khác với ADMIN đang đăng nhập.

Ở luồng ghi, quan sát oldValue/newValue, beforeState/afterState và actor được lấy từ SecurityContext.

F8 đi qua một dòng; F9 chạy tới breakpoint tiếp theo. Có thể học xong luồng đọc với fixture trước, rồi chuyển sang luồng ghi.

**24. Giới hạn của phiên bản thực hành**

- Mẫu ghi chỉ minh họa USER_UPDATED thành công. Chưa tự có lịch sử cho mọi Service.
- Giá trị email/phone/avatar được che; bạn biết trường đã thay đổi nhưng không đọc được nguyên giá trị trước/sau từ audit.
- actor đang là quan hệ tới User. Không xóa cứng tài khoản đã được tham chiếu nếu chưa có chính sách giữ định danh audit phù hợp.
- Các yêu cầu bị từ chối trước khi thay đổi nghiệp vụ không tạo sự kiện SUCCESS.
- requestId/correlationId trong mẫu là operationId nội bộ. Muốn liên kết nhiều bước xử lý thì truyền cùng mã cho các Writer của những bước đó.
- API đọc không cung cấp thao tác sửa/xóa audit. Phân quyền xem lịch sử dùng ROLE_ADMIN như các chức năng quản trị hiện có.
