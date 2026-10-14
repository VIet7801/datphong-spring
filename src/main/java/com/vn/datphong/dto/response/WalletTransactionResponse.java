package com.vn.datphong.dto.response;

import com.vn.datphong.enums.WalletTxType;
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
public class WalletTransactionResponse {
    private Long id;
    private Long walletId;
    private WalletTxType txType;
    private BigDecimal amount;
    private String referenceType;
    private Long referenceId;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
