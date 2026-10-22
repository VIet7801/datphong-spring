package com.vn.datphong.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PriceCalculator {
    public record Breakdown(BigDecimal subtotal, BigDecimal tax, BigDecimal fee, BigDecimal total, int nights) {
    }

    private PriceCalculator() {
    }

    /**
     * @param nightlyRates giá từng đêm trong khoảng [checkIn, checkOut)
     * @param taxRatePercent thuế theo phần trăm của subtotal (0 nếu không áp dụng)
     * @param flatFee phụ phí cố định cho cả booking (0 nếu không áp dụng)
     */
    public static Breakdown compute(List<BigDecimal> nightlyRates, BigDecimal taxRatePercent,
                                    BigDecimal flatFee, String currency) {
        if (nightlyRates == null || nightlyRates.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một đêm");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (BigDecimal rate : nightlyRates) {
            if (rate == null || rate.signum() < 0) {
                throw new IllegalArgumentException("Giá đêm không hợp lệ");
            }
            subtotal = subtotal.add(rate);
        }
        BigDecimal rateOrZero = taxRatePercent == null ? BigDecimal.ZERO : taxRatePercent;
        BigDecimal feeOrZero = flatFee == null ? BigDecimal.ZERO : flatFee;
        BigDecimal roundedSubtotal = CurrencyScale.round(subtotal, currency, RoundingMode.HALF_UP);
        BigDecimal tax = CurrencyScale.round(
                subtotal.multiply(rateOrZero).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP),
                currency, RoundingMode.HALF_UP);
        BigDecimal fee = CurrencyScale.round(feeOrZero, currency, RoundingMode.HALF_UP);
        BigDecimal total = roundedSubtotal.add(tax).add(fee);
        return new Breakdown(roundedSubtotal, tax, fee, total, nightlyRates.size());
    }
}

