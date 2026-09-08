package com.watchdog.domain.port;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Inbound (driving) port: run one poll cycle of the agent loop (spec §3 the agent loop).
 * Implemented by the application layer's PollingService (S4); invoked by the scheduler
 * and, for testing/manual runs, by a controller.
 *
 * <p>A cycle: for each active company, fetch via the right {@link JobSourcePort}, dedup
 * against {@link PostingRepository}, persist genuinely-new postings, and record catch-time.
 * One company/source failure must not abort the cycle (spec §8.5).
 */
public interface PollingUseCase {

    /** Execute one poll cycle across all active companies. */
    PollCycleResult runOnce();

    /**
     * Outcome of a single cycle — the raw material for the agent-status bar and the
     * signature stat (spec §6 / §7 /api/agent/status).
     *
     * @param startedAt        when the cycle began
     * @param companiesPolled  how many companies were contacted
     * @param companiesFailed  how many failed (isolated; cycle continued)
     * @param newPostings      count of genuinely-new postings persisted this cycle
     * @param medianCatchTime  median catch-time of this cycle's new postings, if any had
     *                         a known postedAt (empty otherwise — honest per spec §8.4)
     */
    record PollCycleResult(
            Instant startedAt,
            int companiesPolled,
            int companiesFailed,
            int newPostings,
            Optional<Duration> medianCatchTime
    ) {}
}
