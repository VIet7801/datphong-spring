package com.vn.datphong.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/** Quy tắc làm tròn tiền theo currency. Dùng BigDecimal, không dùng double/float. */
public final class CurrencyScale {
    private static final Set<String> ZERO_DECIMAL = Set.of("VND", "JPY", "KRW");

    private CurrencyScale() {
    }

    public static int of(String currency) {
        return currency != null && ZERO_DECIMAL.contains(currency.toUpperCase()) ? 0 : 2;
    }

    public static BigDecimal round(BigDecimal amount, String currency, RoundingMode mode) {
        return amount.setScale(of(currency), mode);
    }
}
