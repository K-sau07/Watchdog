package com.watchdog.application.polling;

import com.watchdog.domain.port.PostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Keeps the DB fresh by pruning stale postings (D-WD18). Deletes postings the company
 * posted before the retention window, EXCEPT those the user has acted on (saved / applied
 * / hidden — a job_state row protects them, enforced in the repository query). Postings
 * with an unknown posted_at are kept (we can't confirm their age — honest, spec §8.4).
 *
 * <p>Retention window defaults to 21 days ({@code watchdog.retention.days}); basis is
 * posted_at (freshness), not first_seen_at.
 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final PostingRepository postings;
    private final Clock clock;
    private final int retentionDays;

    public RetentionService(PostingRepository postings, Clock clock,
                            @Value("${watchdog.retention.days:21}") int retentionDays) {
        this.postings = postings;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /** Prune stale, unsaved postings. Returns the number deleted. */
    public int prune() {
        Instant cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
        int deleted = postings.deleteStalePostedBefore(cutoff);
        if (deleted > 0) {
            log.info("Retention: pruned {} postings posted before {} ({}d window)",
                    deleted, cutoff, retentionDays);
        }
        return deleted;
    }
}
