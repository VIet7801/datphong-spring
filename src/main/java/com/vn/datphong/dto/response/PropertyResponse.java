package com.vn.datphong.dto.response;

import com.vn.datphong.enums.PropertyStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String slug;
    private String description;
    private String addressLine;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private LocalTime checkInFrom;
    private LocalTime checkOutUntil;
    private String publicContact;
    private PropertyStatus status;
    private Long cancellationPolicyId;
    private String cancellationPolicyName;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
