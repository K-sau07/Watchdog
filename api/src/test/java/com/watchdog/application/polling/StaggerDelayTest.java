package com.watchdog.application.polling;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class StaggerDelayTest {

    @Test
    void noneDoesNothing() {
        // Should return immediately without throwing.
        StaggerDelay.none().pause();
    }

    @Test
    void jitterDelaysWithinRange() {
        // Use a fake RNG returning a fixed fraction; verify Thread.sleep bound is in range
        // by measuring elapsed time for a small window (kept tiny so the test stays fast).
        RandomGenerator half = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public double nextDouble() { return 0.5; }
        };
        long start = System.nanoTime();
        StaggerDelay.jitter(20, 40, half).pause(); // 0.5 -> 30ms
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(20).isLessThan(200); // ~30ms, generous upper bound
    }
}
