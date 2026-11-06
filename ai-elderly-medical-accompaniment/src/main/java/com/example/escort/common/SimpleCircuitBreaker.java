package com.example.escort.common;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SimpleCircuitBreaker {
    private final String name;
    private final int threshold;
    private final long halfOpenMs;
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicLong lastFailTime = new AtomicLong(0);
    private volatile boolean open = false;

    public SimpleCircuitBreaker(String name, int threshold, long halfOpenMs) {
        this.name = name; this.threshold = threshold; this.halfOpenMs = halfOpenMs;
    }

    public boolean allowRequest() {
        if (!open) return true;
        if (System.currentTimeMillis() - lastFailTime.get() >= halfOpenMs) {
            open = false; log.info("熔断器[{}]半开试探", name); return true;
        }
        return false;
    }

    public void onSuccess() { failures.set(0); open = false; }
    public void onFailure() {
        int c = failures.incrementAndGet();
        lastFailTime.set(System.currentTimeMillis());
        if (c >= threshold) { open = true; log.warn("熔断器[{}]打开, 连续失败{}次", name, c); }
    }
    public boolean isOpen() { return open; }
}