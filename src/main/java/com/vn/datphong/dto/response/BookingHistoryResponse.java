package com.vn.datphong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHistoryResponse {
    private String from;

    private String to;

    private String source;

    private Long actorId;

    private String reason;

    private LocalDateTime at;
}
