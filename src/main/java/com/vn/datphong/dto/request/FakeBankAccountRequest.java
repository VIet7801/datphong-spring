package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeBankAccountRequest {
    @NotBlank
    @Size(max = 40)
    private String accountNo;

    @NotBlank
    @Size(max = 120)
    private String holderName;

    @NotNull
    private BigDecimal balance;
}