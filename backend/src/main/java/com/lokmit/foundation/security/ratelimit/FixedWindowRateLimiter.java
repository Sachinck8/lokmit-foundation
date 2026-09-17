package com.lokmit.foundation.security.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Dependency-free, thread-safe, fixed-window request rate limiter (I-6).
 *
 * <p>Each tracked key holds one {@link Window}: a counter plus the window
 * start timestamp. A request consumes one permit; when the window elapses the
 * counter resets. Windows are keyed by client identity (IP address) and each
 * protected endpoint category owns its own limiter instance, so limits are
 * independent.</p>
 *
 * <p><strong>Memory safety:</strong> the number of tracked keys is bounded by
 * {@code maxTrackedKeys}. When the bound is reached the limiter first evicts
 * expired windows and, if the map is still full, evicts the window with the
 * oldest start — so an attacker rotating spoofed IPs (in a misconfigured
 * proxy setup) or a large NAT pool cannot grow the map without bound.</p>
 *
 * <p><strong>Concurrency:</strong> consumption is decided inside a
 * {@code synchronized} block on the per-key window object, so N concurrent
 * requests for one key can consume at most {@code capacity} permits — the
 * limit cannot be bypassed by parallel requests. Contention is per-key only.</p>
 *
 * <p><strong>Instance-local:</strong> state lives in this JVM's heap only.
 * The limit is per application instance, not cluster-wide. The class is
 * deliberately self-contained (one {@code tryConsume} entry point, injectable
 * clock) so a Redis/DB-backed implementation can replace it later without
 * touching callers.</p>
 */
public class FixedWindowRateLimiter {

    /** Result of one consumption attempt. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision allow() {
            return new Decision(true, 0);
        }
    }

    /** Mutable fixed-window state for a single key. */
    private static final class Window {
        long windowStartMillis;
        long count;

        Window(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }

    private final int capacity;
    private final long windowMillis;
    private final int maxTrackedKeys;
    private final LongSupplier clockMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @param capacity       requests allowed per window (must be >= 1)
     * @param window         window length (must be positive)
     * @param maxTrackedKeys upper bound on concurrently tracked client keys
     * @param clockMillis    monotonic wall-clock supplier (injectable for tests)
     */
    public FixedWindowRateLimiter(int capacity, Duration window, int maxTrackedKeys, LongSupplier clockMillis) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Rate-limit capacity must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }
        this.capacity = capacity;
        this.windowMillis = window.toMillis();
        this.maxTrackedKeys = maxTrackedKeys;
        this.clockMillis = clockMillis;
    }

    /**
     * Attempts to consume one permit for {@code key}.
     *
     * @return {@link Decision#allow()} when under the limit, otherwise a denial
     *         carrying the number of seconds until the current window resets
     *         (at least 1).
     */
    public Decision tryConsume(String key) {
        long now = clockMillis.getAsLong();
        Window window = obtainWindow(key, now);
        synchronized (window) {
            if (now - window.windowStartMillis >= windowMillis) {
                // New window for this key: reset in place (keeps map size stable).
                window.windowStartMillis = now;
                window.count = 0;
            }
            if (window.count < capacity) {
                window.count++;
                return Decision.allow();
            }
            long millisUntilReset = window.windowStartMillis + windowMillis - now;
            long retryAfterSeconds = Math.max(1L, (long) Math.ceil(millisUntilReset / 1000.0));
            return new Decision(false, retryAfterSeconds);
        }
    }

    /** Number of currently tracked keys (exposed for tests and monitoring). */
    synchronized int trackedKeyCount() {
        return windows.size();
    }

    /**
     * Returns the window for {@code key}, creating it only when the tracker
     * has room. Enforces the memory bound: expired windows are purged first;
     * if the map is still full the oldest-start window is evicted.
     */
    private Window obtainWindow(String key, long now) {
        Window existing = windows.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = windows.get(key);
            if (existing != null) {
                return existing;
            }
            if (windows.size() >= maxTrackedKeys) {
                evict(now);
            }
            Window created = new Window(now);
            windows.put(key, created);
            return created;
        }
    }

    /** Purges expired windows; evicts oldest-start windows if still full. */
    private void evict(long now) {
        windows.values().removeIf(window -> now - window.windowStartMillis >= windowMillis);
        while (windows.size() >= maxTrackedKeys) {
            String oldestKey = null;
            long oldestStart = Long.MAX_VALUE;
            for (Map.Entry<String, Window> entry : windows.entrySet()) {
                if (entry.getValue().windowStartMillis < oldestStart) {
                    oldestStart = entry.getValue().windowStartMillis;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey == null) {
                break;
            }
            windows.remove(oldestKey);
        }
    }
}
