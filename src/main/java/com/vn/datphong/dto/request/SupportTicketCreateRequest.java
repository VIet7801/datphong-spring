package com.vn.datphong.dto.request;

import com.vn.datphong.enums.TicketTypeEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketCreateRequest {

    private Long bookingId;

    @NotNull(message = "Loại khiếu nại không được để trống")
    private TicketTypeEnum ticketType;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;

    /** Người bị khiếu nại (VD: host báo khách làm hỏng đồ -> đây là userId của khách). Có thể để trống. */
    private Long againstUserId;
}
