package com.vn.datphong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostVipPackageResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer durationDays;
    private Map<String, Object> benefits;
}
