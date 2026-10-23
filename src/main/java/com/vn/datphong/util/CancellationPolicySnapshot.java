package com.vn.datphong.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ảnh chụp chính sách hủy tại thời điểm đặt, lưu JSON trong bookings.cancellation_policy_snapshot
 * nên HOST sửa chính sách/giờ check-in về sau không làm đổi phí của booking đã tạo.
 *
 * Snapshot mới dùng JSON; vẫn đọc định dạng v1|... của code cũ và JSON do bộ seed tạo.
 * Quy tắc: tính số ngày đầy đủ từ lúc hủy đến mốc nhận phòng (theo múi giờ cơ sở); chọn rule có
 * daysBefore lớn nhất mà số ngày còn lại >= daysBefore. Không rule nào khớp hoặc đã quá mốc nhận phòng -> hoàn 0%.
 */
public final class CancellationPolicySnapshot {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public record Rule(int daysBeforeCheckin, BigDecimal refundPercent) {
    }

    private final String name;
    private final ZoneId zone;
    private final LocalTime checkInTime;
    private final List<Rule> rules;

    private CancellationPolicySnapshot(String name, ZoneId zone, LocalTime checkInTime, List<Rule> rules) {
        this.name = name;
        this.zone = zone;
        this.checkInTime = checkInTime;
        this.rules = rules;
    }

    public static CancellationPolicySnapshot of(String name, ZoneId zone, LocalTime checkInTime, List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules == null ? List.of() : rules);
        for (Rule r : sorted) {
            if (r.daysBeforeCheckin() < 0) {
                throw new IllegalArgumentException("daysBeforeCheckin phải >= 0");
            }
            if (r.refundPercent().signum() < 0 || r.refundPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("refundPercent phải trong [0,100]");
            }
        }
        sorted.sort(Comparator.comparingInt(Rule::daysBeforeCheckin).reversed());
        String safeName = (name == null ? "" : name).replace("|", " ").replace(",", " ").trim();
        return new CancellationPolicySnapshot(safeName, zone, checkInTime == null ? LocalTime.of(14, 0) : checkInTime,
                List.copyOf(sorted));
    }

    public String serialize() {
        return JSON.writeValueAsString(Map.of("revision", 1, "name", name, "timezone", zone.getId(),
                "checkinLocal", checkInTime.toString(), "rules", rules));
    }

    public static CancellationPolicySnapshot parse(String text) {
        if (text == null || text.isBlank()) {
            // Không có snapshot: không hoàn tiền là hướng an toàn cho nền tảng, nhưng báo rõ để hiển thị.
            return of("Không hoàn tiền", ZoneId.of("UTC"), LocalTime.of(14, 0), List.of());
        }
        text = text.trim();
        if (!text.startsWith("v1|")) {
            return parseJson(text);
        }
        String[] parts = text.split("\\|", -1);
        if (parts.length != 5 || !"v1".equals(parts[0])) {
            throw new IllegalArgumentException("Snapshot chính sách hủy không hợp lệ");
        }
        List<Rule> parsed = new ArrayList<>();
        if (!parts[4].isBlank()) {
            for (String item : parts[4].split(",")) {
                String[] kv = item.split(":");
                parsed.add(new Rule(Integer.parseInt(kv[0].trim()), new BigDecimal(kv[1].trim())));
            }
        }
        return of(parts[1], ZoneId.of(parts[2]), LocalTime.parse(parts[3]), parsed);
    }

    private static CancellationPolicySnapshot parseJson(String text) {
        try {
            JsonNode node = JSON.readTree(text);
            if (node.isTextual()) return parse(node.asText());
            if (!node.isObject() || node.path("revision").asInt(-1) != 1
                    || !node.path("timezone").isTextual() || !node.path("checkinLocal").isTextual()
                    || !node.path("rules").isArray()) {
                throw new IllegalArgumentException("Snapshot chính sách hủy không hợp lệ");
            }
            List<Rule> parsed = new ArrayList<>();
            for (JsonNode rule : node.path("rules")) {
                if (!rule.path("daysBeforeCheckin").isIntegralNumber()
                        || !rule.path("refundPercent").isNumber()) {
                    throw new IllegalArgumentException("Rule chính sách hủy không hợp lệ");
                }
                parsed.add(new Rule(rule.path("daysBeforeCheckin").intValue(),
                        new BigDecimal(rule.path("refundPercent").asText())));
            }
            String name = node.hasNonNull("name") ? node.path("name").asText()
                    : node.path("policyCode").asText("Chính sách hủy");
            return of(name, ZoneId.of(node.path("timezone").asText()),
                    LocalTime.parse(node.path("checkinLocal").asText()), parsed);
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Snapshot chính sách hủy không hợp lệ", invalid);
        }
    }

    public String getName() {
        return name;
    }

    public ZoneId getZone() {
        return zone;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public List<Rule> getRules() {
        return rules;
    }

    /** Mốc bắt đầu nhận phòng theo múi giờ cơ sở. */
    public Instant checkInInstant(LocalDate checkinDate) {
        return checkinDate.atTime(checkInTime).atZone(zone).toInstant();
    }

    /** Phần trăm hoàn lại nếu hủy tại thời điểm now. */
    public BigDecimal refundPercentAt(Instant now, LocalDate checkinDate) {
        Instant start = checkInInstant(checkinDate);
        if (!now.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        long wholeDays = Duration.between(now, start).toHours() / 24;
        for (Rule rule : rules) {
            if (wholeDays >= rule.daysBeforeCheckin()) {
                return rule.refundPercent();
            }
        }
        return BigDecimal.ZERO;
    }

    public record Result(BigDecimal refundPercent, BigDecimal refundAmount, BigDecimal feeAmount) {
    }

    /**
     * @param paidNet số tiền thực thu còn lại (đã thu trừ đã hoàn). Làm tròn xuống khi hoàn để không bao giờ hoàn vượt.
     */
    public Result evaluate(Instant now, LocalDate checkinDate, BigDecimal paidNet, String currency) {
        if (paidNet == null || paidNet.signum() <= 0) {
            return new Result(BigDecimal.ZERO, CurrencyScale.round(BigDecimal.ZERO, currency, RoundingMode.DOWN),
                    CurrencyScale.round(BigDecimal.ZERO, currency, RoundingMode.DOWN));
        }
        BigDecimal pct = refundPercentAt(now, checkinDate);
        BigDecimal refund = CurrencyScale.round(
                paidNet.multiply(pct).divide(BigDecimal.valueOf(100), 6, RoundingMode.DOWN), currency, RoundingMode.DOWN);
        if (refund.compareTo(paidNet) > 0) {
            refund = CurrencyScale.round(paidNet, currency, RoundingMode.DOWN);
        }
        BigDecimal fee = CurrencyScale.round(paidNet, currency, RoundingMode.DOWN).subtract(refund);
        return new Result(pct, refund, fee);
    }
}
