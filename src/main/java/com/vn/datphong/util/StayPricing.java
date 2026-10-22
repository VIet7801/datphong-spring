package com.vn.datphong.util;

import com.vn.datphong.entity.CancellationPolicy;
import com.vn.datphong.entity.CancellationPolicyRule;
import com.vn.datphong.entity.Property;
import com.vn.datphong.entity.RoomRate;
import com.vn.datphong.exception.DomainException;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StayPricing {

    private StayPricing() {
    }

    public record PricedStay(List<RoomRate> nights, PriceCalculator.Breakdown breakdown, String currency, long priceVersion) {
    }

    /** Yêu cầu đủ giá cho MỌI đêm trong [checkIn, checkOut), không đêm nào đóng bán, cùng một currency. */
    public static PricedStay price(List<RoomRate> rates, LocalDate checkIn, LocalDate checkOut,
                                   BigDecimal taxRatePercent, BigDecimal flatFee) {
        Set<LocalDate> have = new HashSet<>();
        for (RoomRate r : rates) {
            have.add(r.getStayDate());
        }
        List<String> missing = new ArrayList<>();
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            if (!have.contains(d)) {
                missing.add(d.toString());
            }
        }
        if (!missing.isEmpty()) {
            throw DomainException.conflict("RATE_NOT_CONFIGURED", "Chưa cấu hình giá cho một số đêm", missing);
        }
        String currency = rates.get(0).getCurrency();
        long version = 0;
        List<BigDecimal> amounts = new ArrayList<>();
        for (RoomRate r : rates) {
            if (Boolean.TRUE.equals(r.getClosedToSale())) {
                throw DomainException.conflict("ROOM_UNAVAILABLE", "Có đêm đang đóng bán: " + r.getStayDate());
            }
            if (!currency.equals(r.getCurrency())) {
                throw DomainException.conflict("RATE_NOT_CONFIGURED", "Các đêm dùng nhiều loại tiền tệ khác nhau");
            }
            version = Math.max(version, r.getPriceVersion());
            amounts.add(r.getNightlyRate());
        }
        return new PricedStay(rates, PriceCalculator.compute(amounts, taxRatePercent, flatFee, currency), currency, version);
    }

    /** @param rules các luật của chính sách mà cơ sở đang gán (rỗng nếu chưa có chính sách). */
    public static CancellationPolicySnapshot snapshot(Property property, List<CancellationPolicyRule> rules) {
        ZoneId zone;
        try {
            zone = ZoneId.of(property.getTimezone());
        } catch (DateTimeException e) {
            zone = ZoneId.of("UTC");
        }
        LocalTime checkInFrom = property.getCheckInFrom() == null ? LocalTime.of(14, 0) : property.getCheckInFrom();
        CancellationPolicy policy = property.getCancellationPolicy();
        if (policy == null) {
            return CancellationPolicySnapshot.of("Không hoàn tiền", zone, checkInFrom, List.of());
        }
        List<CancellationPolicySnapshot.Rule> out = new ArrayList<>();
        for (CancellationPolicyRule r : rules) {
            out.add(new CancellationPolicySnapshot.Rule(r.getDaysBeforeCheckin(), r.getRefundPercent()));
        }
        return CancellationPolicySnapshot.of(policy.getName(), zone, checkInFrom, out);
    }
}

