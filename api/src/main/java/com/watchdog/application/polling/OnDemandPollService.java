package com.watchdog.application.polling;

import com.watchdog.domain.port.PollingUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * On-demand ("refresh now") polling with a cooldown (D-WD19). Lets the user trigger an
 * immediate poll while actively hunting, without waiting for the hourly background cycle —
 * but rate-limited so repeated clicks can't recreate the burst signature we slowed the
 * background loop to avoid. Cooldown defaults to 120s ({@code watchdog.polling.on-demand-cooldown-ms}).
 *
 * <p>Thread-safe via a CAS on the last-run timestamp: concurrent clicks yield exactly one
 * poll. ShedLock still guards against a background cycle running simultaneously.
 */
@Service
public class OnDemandPollService {

    private final PollingUseCase polling;
    private final Clock clock;
    private final long cooldownMs;
    private final AtomicReference<Instant> lastRun = new AtomicReference<>(Instant.EPOCH);

    public OnDemandPollService(PollingUseCase polling, Clock clock,
                               @Value("${watchdog.polling.on-demand-cooldown-ms:120000}") long cooldownMs) {
        this.polling = polling;
        this.clock = clock;
        this.cooldownMs = cooldownMs;
    }

    /** Result of a refresh attempt: either it ran, or it's cooling down. */
    public sealed interface Result {
        record Ran(int companiesPolled, int newPostings) implements Result {}
        record CoolingDown(long retryAfterSeconds) implements Result {}
    }

    /**
     * Trigger a poll now if the cooldown has elapsed; otherwise report the wait. Only the
     * caller that wins the CAS actually polls, so concurrent requests don't stack cycles.
     */
    public Result refreshNow() {
        Instant now = Instant.now(clock);
        Instant last = lastRun.get();
        long sinceMs = Duration.between(last, now).toMillis();
        if (sinceMs < cooldownMs) {
            long retryAfter = (cooldownMs - sinceMs + 999) / 1000; // round up to whole seconds
            return new Result.CoolingDown(retryAfter);
        }
        // Claim the slot; if another thread claimed it first, treat as cooling down.
        if (!lastRun.compareAndSet(last, now)) {
            return new Result.CoolingDown((cooldownMs + 999) / 1000);
        }
        var cycle = polling.runOnce();
        return new Result.Ran(cycle.companiesPolled(), cycle.newPostings());
    }
}
