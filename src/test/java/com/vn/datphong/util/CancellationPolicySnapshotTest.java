package com.vn.datphong.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class CancellationPolicySnapshotTest {
    @Test
    void newSnapshotIsAJsonObjectAndPreservesRefundCalculation() {
        var original = CancellationPolicySnapshot.of("Linh hoạt \"A\"", ZoneId.of("Asia/Bangkok"),
                LocalTime.of(14, 0), List.of(
                        new CancellationPolicySnapshot.Rule(7, new BigDecimal("100")),
                        new CancellationPolicySnapshot.Rule(2, new BigDecimal("50.25")),
                        new CancellationPolicySnapshot.Rule(0, BigDecimal.ZERO)));
        String json = original.serialize();
        assertTrue(JsonMapper.builder().build().readTree(json).isObject());
        var restored = CancellationPolicySnapshot.parse(json);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getZone(), restored.getZone());
        assertEquals(original.getCheckInTime(), restored.getCheckInTime());
        assertEquals(original.getRules(), restored.getRules());
        var date = LocalDate.of(2027, 1, 10);
        var now = restored.checkInInstant(date).minus(Duration.ofDays(3));
        assertEquals(original.evaluate(now, date, new BigDecimal("100000"), "VND"),
                restored.evaluate(now, date, new BigDecimal("100000"), "VND"));
        assertEquals(new BigDecimal("50.25"), restored.refundPercentAt(now, date));
        assertEquals(BigDecimal.ZERO, restored.refundPercentAt(restored.checkInInstant(date), date));
    }

    @Test
    void oldPipeFormatCanStillBeReadAndSerializedAsJson() {
        var snapshot = CancellationPolicySnapshot.parse("v1|Linh hoạt|Asia/Bangkok|14:00|7:100,2:50,0:0");
        assertEquals("Linh hoạt", snapshot.getName());
        assertEquals(3, snapshot.getRules().size());
        assertTrue(JsonMapper.builder().build().readTree(snapshot.serialize()).isObject());
        var quoted = JsonMapper.builder().build().writeValueAsString("v1|Legacy|UTC|14:00|0:0");
        assertEquals("Legacy", CancellationPolicySnapshot.parse(quoted).getName());
    }

    @Test
    void existingSeedJsonKeepsTimezoneAndRules() {
        var snapshot = CancellationPolicySnapshot.parse("""
                {"policyCode":"DEMO-FLEX","revision":1,"timezone":"Asia/Bangkok","checkinLocal":"14:00",
                 "rules":[{"daysBeforeCheckin":7,"refundPercent":100},{"daysBeforeCheckin":2,"refundPercent":50}],
                 "currency":"VND","taxPercent":8,"serviceFeePercent":5}
                """);
        assertEquals("DEMO-FLEX", snapshot.getName());
        assertEquals(ZoneId.of("Asia/Bangkok"), snapshot.getZone());
        assertEquals(2, snapshot.getRules().size());
    }

    @Test
    void invalidJsonOrUnsupportedRevisionIsRejected() {
        for (String text : List.of("not json", "{}", "null", "[]",
                "{\"revision\":2,\"timezone\":\"UTC\",\"checkinLocal\":\"14:00\",\"rules\":[]}",
                "{\"revision\":1,\"timezone\":\"UTC\",\"checkinLocal\":\"14:00\",\"rules\":[{\"daysBeforeCheckin\":0,\"refundPercent\":101}]}")) {
            assertThrows(IllegalArgumentException.class, () -> CancellationPolicySnapshot.parse(text));
        }
    }
}
