package com.vn.datphong.dto.request;

import com.vn.datphong.enums.RoomUnitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUnitCreateRequest {
    @NotNull
    private Long propertyId;

    @NotNull
    private Long roomTypeId;

    @NotBlank
    @Size(max = 50)
    private String code;

    private RoomUnitStatus status;

    @Size(max = 50)
    private String floor;

    private String notes;
}
