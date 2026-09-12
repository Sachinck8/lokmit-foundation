package com.lokmit.foundation.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link FixedWindowRateLimiter} core: capacity, window
 * reset, per-key isolation, Retry-After computation, memory bound/eviction,
 * and concurrency safety. No Spring context needed.
 */
class FixedWindowRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);

    private FixedWindowRateLimiter limiter(int capacity, int windowSeconds, int maxKeys) {
        return new FixedWindowRateLimiter(capacity, Duration.ofSeconds(windowSeconds), maxKeys,
                () -> now.get());
    }

    @Test
    @DisplayName("allows requests up to capacity, then denies")
    void allowsUpToCapacity() {
        FixedWindowRateLimiter limiter = limiter(3, 60, 100);

        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();

        FixedWindowRateLimiter.Decision denied = limiter.tryConsume("ip1");
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("window reset restores capacity after the configured window")
    void resetsAfterWindow() {
        FixedWindowRateLimiter limiter = limiter(2, 30, 100);

        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isFalse();

        now.addAndGet(Duration.ofSeconds(30).toMillis()); // window elapsed

        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isFalse();
    }

    @Test
    @DisplayName("Retry-After decreases as the window elapses and never goes below 1")
    void retryAfterShrinksWithElapsedTime() {
        FixedWindowRateLimiter limiter = limiter(1, 60, 100);

        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        long immediately = limiter.tryConsume("ip1").retryAfterSeconds();
        assertThat(immediately).isEqualTo(60L);

        now.addAndGet(Duration.ofSeconds(59).toMillis());
        long almostReset = limiter.tryConsume("ip1").retryAfterSeconds();
        assertThat(almostReset).isEqualTo(1L);
    }

    @Test
    @DisplayName("different keys have independent windows")
    void keysAreIndependent() {
        FixedWindowRateLimiter limiter = limiter(1, 60, 100);

        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isFalse();

        assertThat(limiter.tryConsume("ip2").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip3").allowed()).isTrue();
    }

    @Test
    @DisplayName("half-consumed window carries the remainder into denial decisions only")
    void partialWindowDoesNotLeakIntoNext() {
        FixedWindowRateLimiter limiter = limiter(2, 60, 100);

        limiter.tryConsume("ip1");
        now.addAndGet(Duration.ofSeconds(59).toMillis());
        limiter.tryConsume("ip1"); // still window 1
        now.addAndGet(Duration.ofSeconds(2).toMillis()); // window 2 begins

        // Fresh window: two more permits available.
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip1").allowed()).isFalse();
    }

    @Test
    @DisplayName("expired entries are evicted so the tracker does not grow unbounded")
    void evictsExpiredEntriesWhenFull() {
        FixedWindowRateLimiter limiter = limiter(1, 60, 3);

        limiter.tryConsume("ip1");
        limiter.tryConsume("ip2");
        limiter.tryConsume("ip3");
        assertThat(limiter.trackedKeyCount()).isEqualTo(3);

        now.addAndGet(Duration.ofSeconds(61).toMillis()); // all expired
        limiter.tryConsume("ip4"); // triggers eviction of expired windows

        assertThat(limiter.trackedKeyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("oldest-start window is evicted when all entries are still live")
    void evictsOldestWhenFullOfLiveEntries() {
        FixedWindowRateLimiter limiter = limiter(100, 3600, 2);

        limiter.tryConsume("ip1"); // oldest
        now.addAndGet(1000);
        limiter.tryConsume("ip2");
        now.addAndGet(1000);
        limiter.tryConsume("ip3"); // forces eviction of ip1

        assertThat(limiter.trackedKeyCount()).isEqualTo(2);
        // ip1's window was evicted, so it starts fresh and is allowed again.
        assertThat(limiter.tryConsume("ip1").allowed()).isTrue();
    }

    @Test
    @DisplayName("concurrent consumers cannot exceed capacity")
    void concurrencyCannotBypassCapacity() throws Exception {
        int capacity = 25;
        FixedWindowRateLimiter limiter = limiter(capacity, 60, 1000);

        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return limiter.tryConsume("same-ip").allowed();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long allowed = results.stream().filter(f -> {
                try {
                    return f.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).count();

            assertThat(allowed).isEqualTo(capacity);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("rejects invalid constructor arguments")
    void rejectsInvalidConfiguration() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FixedWindowRateLimiter(0, Duration.ofSeconds(60), 10, () -> 0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FixedWindowRateLimiter(5, Duration.ZERO, 10, () -> 0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FixedWindowRateLimiter(5, Duration.ofSeconds(-1), 10, () -> 0L));
    }
}
