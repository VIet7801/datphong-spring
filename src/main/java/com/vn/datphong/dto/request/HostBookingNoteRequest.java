package com.vn.datphong.dto.request;

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
public class HostBookingNoteRequest {
    @Size(max = 500)
    private String hostNote;

    @NotNull
    private Long version;
}
