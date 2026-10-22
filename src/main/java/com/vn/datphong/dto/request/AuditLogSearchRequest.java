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