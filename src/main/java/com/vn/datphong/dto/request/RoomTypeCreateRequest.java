package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeCreateRequest {
    @NotNull
    private Long propertyId;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    @Min(1)
    private Integer maxGuests;

    @Min(0)
    private Integer maxAdults;

    @Min(0)
    private Integer maxChildren;

    @Size(max = 100)
    private String bedType;

    private String description;
    private Long cancellationPolicyId;
}
