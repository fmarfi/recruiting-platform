package com.recruiting.platform.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Public API of the security module: fixed-window request counter per key (e.g. client IP), in memory.
 * Enough for the pilot (one instance, ADR 0007); swap for a shared store if the app scales out.
 */
@Component
public class RequestRateLimiter {

    private record Window(long start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one request for {@code key}; returns false when more than {@code max} came within {@code window}. */
    public boolean tryAcquire(String key, int max, Duration window) {
        long now = clock.millis();
        if (windows.size() > 10_000) {
            windows.values().removeIf(w -> now - w.start() > window.toMillis());
        }
        Window result = windows.merge(key, new Window(now, 1),
                (old, fresh) -> now - old.start() >= window.toMillis() ? fresh : new Window(old.start(), old.count() + 1));
        return result.count() <= max;
    }
}
