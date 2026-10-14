package com.vn.datphong.dto.response;


import com.vn.datphong.entity.LoyaltyRank;
import com.vn.datphong.enums.DiscountTypeEnum;
import com.vn.datphong.enums.VoucherOwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private VoucherOwnerType ownerType;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private Integer minNights;
    private BigDecimal minOrderValue;
    private LoyaltyRank targetRank;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer usageLimitPerUser;
    private LocalDateTime createdAt;
}
