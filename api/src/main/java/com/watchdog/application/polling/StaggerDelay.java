package com.watchdog.application.polling;

import java.util.random.RandomGenerator;

/**
 * Pluggable delay between board requests, so the background poll cycle spreads its
 * requests over time instead of firing all boards in one burst (D-WD20 — avoids the
 * bot-block signature). Abstracted so tests can verify the jitter logic without actually
 * sleeping, and so on-demand polls can use a no-op (a single user clicking once is not a
 * burst).
 */
public interface StaggerDelay {

    /** Wait a jittered interval before the next request. No-op for the no-stagger strategy. */
    void pause();

    /** No delay — for on-demand refresh (not a burst) and tests. */
    static StaggerDelay none() {
        return () -> { };
    }

    /**
     * Real jitter: sleeps a uniformly-random duration in [minMs, maxMs] before each
     * request. Randomized (not fixed) so the request rhythm doesn't look mechanical.
     */
    static StaggerDelay jitter(long minMs, long maxMs, RandomGenerator rng) {
        return () -> {
            long delay = minMs + (long) (rng.nextDouble() * (maxMs - minMs));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
