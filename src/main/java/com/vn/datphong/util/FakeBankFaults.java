package com.vn.datphong.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FakeBankFaults {
    private final AtomicInteger refundFailures = new AtomicInteger();
    private final AtomicInteger refundTimeouts = new AtomicInteger();

    public void set(int failures, int timeouts) {
        refundFailures.set(Math.max(0, failures));
        refundTimeouts.set(Math.max(0, timeouts));
    }

    public boolean consumeRefundFailure() {
        return refundFailures.getAndUpdate(v -> v > 0 ? v - 1 : 0) > 0;
    }

    public boolean consumeRefundTimeout() {
        return refundTimeouts.getAndUpdate(v -> v > 0 ? v - 1 : 0) > 0;
    }

    public int pendingFailures() {
        return refundFailures.get();
    }

    public int pendingTimeouts() {
        return refundTimeouts.get();
    }
}
