package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyCreateRequest {
    @NotBlank
    @Size(max = 180)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String addressLine;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 64)
    private String timezone;

    private LocalTime checkInFrom;
    private LocalTime checkOutUntil;

    @Size(max = 255)
    private String publicContact;

    private String description;
    private Long cancellationPolicyId;
}
