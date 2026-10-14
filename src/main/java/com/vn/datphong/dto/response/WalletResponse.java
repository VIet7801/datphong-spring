package com.vn.datphong.dto.response;

import com.vivu.booking.enums.WalletOwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private Long id;
    private Long ownerId;
    private WalletOwnerType ownerType;
    private BigDecimal balance;
    private String currency;
}
