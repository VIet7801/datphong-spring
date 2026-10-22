package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionedReasonRequest extends StrictRequest {
    @NotNull
    @PositiveOrZero
    private Long version;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
