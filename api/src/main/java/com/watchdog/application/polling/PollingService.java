package com.watchdog.application.polling;

import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.JobSourcePort;
import com.watchdog.domain.port.PollingUseCase;
import com.watchdog.domain.port.PostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The agent loop's core (spec §3). One cycle: for each active company, route to its
 * ATS adapter, fetch postings, dedup against what we've already seen, persist the
 * genuinely-new ones, and record catch-time. Pure orchestration over ports — no HTTP,
 * DB, scheduler, or lock here (those are infrastructure, wired in S4.3/S4.4).
 *
 * <p>Resilience (spec §8.5): a single company/source failure is caught, logged, and
 * counted — the cycle continues. Dedup (spec §8.3): a posting is new exactly once, by
 * its natural key.
 */
@Service
public class PollingService implements PollingUseCase {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final AtsSourceRouter router;
    private final CompanyRepository companies;
    private final PostingRepository postings;
    private final Clock clock;
    private final StaggerDelay backgroundStagger;

    public PollingService(AtsSourceRouter router, CompanyRepository companies,
                          PostingRepository postings, Clock clock,
                          @org.springframework.beans.factory.annotation.Value("${watchdog.polling.stagger-min-ms:3000}") long staggerMinMs,
                          @org.springframework.beans.factory.annotation.Value("${watchdog.polling.stagger-max-ms:15000}") long staggerMaxMs) {
        this.router = router;
        this.companies = companies;
        this.postings = postings;
        this.clock = clock;
        this.backgroundStagger = StaggerDelay.jitter(
                staggerMinMs, staggerMaxMs, java.util.concurrent.ThreadLocalRandom.current());
    }

    @Override
    public PollCycleResult runOnce() {
        // No stagger — on-demand refresh (one user, one click) isn't a burst.
        return runCycle(StaggerDelay.none());
    }

    @Override
    public PollCycleResult runStaggered() {
        return runCycle(backgroundStagger);
    }

    private PollCycleResult runCycle(StaggerDelay delay) {
        Instant startedAt = Instant.now(clock);
        List<Company> active = companies.findActive();
        int polled = 0;
        int failed = 0;
        List<Posting> newlyPersisted = new ArrayList<>();

        boolean first = true;
        for (Company company : active) {
            if (!first) {
                delay.pause(); // spread requests over time (D-WD20); no-op when not staggering
            }
            first = false;
            try {
                newlyPersisted.addAll(pollCompany(company));
                polled++;
            } catch (Exception e) {
                // Isolate: one company/source failure never aborts the cycle (§8.5).
                failed++;
                log.warn("Poll failed for company {} ({} / {}): {}",
                        company.id(), company.atsSource(), company.atsSlug(), e.toString());
            }
        }

        PollCycleResult result = new PollCycleResult(
                startedAt, polled, failed, newlyPersisted.size(),
                medianCatchTime(newlyPersisted));
        log.info("Poll cycle done: {} polled, {} failed, {} new postings",
                polled, failed, newlyPersisted.size());
        return result;
    }

    /** Fetch one company's board and persist only postings we haven't seen before. */
    private List<Posting> pollCompany(Company company) {
        JobSourcePort port = router.routeTo(company.atsSource());
        List<Posting> fetched = port.fetchPostings(company);

        List<Posting> newlyPersisted = new ArrayList<>();
        for (Posting posting : fetched) {
            if (!postings.existsByNaturalKey(posting.companyId(), posting.atsPostingId())) {
                newlyPersisted.add(postings.save(posting));
            }
        }
        // Mark the company polled so registry/status reflects real activity.
        companies.save(company.polledAt(Instant.now(clock)));
        return newlyPersisted;
    }

    /**
     * Median catch-time across this cycle's new postings that have a known postedAt.
     * Empty when none do — honest, never faked (spec §8.4).
     */
    private static Optional<Duration> medianCatchTime(List<Posting> newPostings) {
        List<Duration> catches = newPostings.stream()
                .map(Posting::catchTime)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (catches.isEmpty()) {
            return Optional.empty();
        }
        int mid = catches.size() / 2;
        if (catches.size() % 2 == 1) {
            return Optional.of(catches.get(mid));
        }
        // even count: average the two middle values
        Duration lo = catches.get(mid - 1);
        Duration hi = catches.get(mid);
        return Optional.of(lo.plus(hi).dividedBy(2));
    }
}
