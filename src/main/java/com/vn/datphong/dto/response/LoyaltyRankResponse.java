package com.vn.datphong.dto.response;

import com.vn.datphong.enums.RankNameType;
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
public class LoyaltyRankResponse {
    private Long id;
    private RankNameType name;
    private Integer minPoints;
    private BigDecimal discountPercent;
    /** VD: {"free_upgrade": true, "birthday_voucher": true} */
    private Map<String, Object> benefits;
}
