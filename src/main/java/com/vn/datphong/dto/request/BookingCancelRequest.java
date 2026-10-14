package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancelRequest {
    @NotBlank
    private String idempotencyKey;

    @Size(max = 500)
    private String reason;
}
