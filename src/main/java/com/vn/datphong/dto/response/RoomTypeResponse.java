package com.vn.datphong.dto.response;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private String code;
    private String name;
    private Integer maxGuests;
    private Integer maxAdults;
    private Integer maxChildren;
    private String bedType;
    private String description;
    private Long cancellationPolicyId;
    private String cancellationPolicyName;
    private Boolean active;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
