package com.watchdog.application.matching;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.PostingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Computes the agent-status snapshot for the dashboard's status bar (spec §7
 * {@code GET /api/agent/status}, §6 the "live agent" + signature stat). DB-derived
 * (D-WD12): every number reflects what's actually persisted — no in-memory cycle
 * state to drift or reset. The only non-DB value is the next-poll estimate
 * (last poll + interval), which is arithmetic, not a fabricated observation.
 */
@Service
public class AgentStatusService {

    private final CompanyRepository companies;
    private final PostingRepository postings;
    private final Clock clock;
    private final long pollIntervalMs;

    public AgentStatusService(CompanyRepository companies, PostingRepository postings, Clock clock,
                              @Value("${watchdog.polling.interval-ms:120000}") long pollIntervalMs) {
        this.companies = companies;
        this.postings = postings;
        this.clock = clock;
        this.pollIntervalMs = pollIntervalMs;
    }

    /**
     * Snapshot of agent activity: how many boards are watched (per source), the last and
     * estimated-next poll, and today's freshness numbers (new count + median catch-time).
     */
    public AgentStatus current() {
        Instant now = Instant.now(clock);
        List<Company> active = companies.findActive();

        Map<AtsSource, Long> perSource = active.stream()
                .collect(Collectors.groupingBy(Company::atsSource, Collectors.counting()));

        // Last poll = the most recent company poll we've recorded (honest; null if never).
        Instant lastPoll = active.stream()
                .map(Company::lastPolledAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Instant nextPoll = lastPoll == null ? null : lastPoll.plusMillis(pollIntervalMs);

        // Today's window (start of the current UTC day) → new count + median catch-time.
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        List<Posting> today = postings.findSeenSince(startOfDay);

        return new AgentStatus(
                active.size(),
                perSource.getOrDefault(AtsSource.GREENHOUSE, 0L),
                perSource.getOrDefault(AtsSource.LEVER, 0L),
                perSource.getOrDefault(AtsSource.ASHBY, 0L),
                lastPoll,
                nextPoll,
                today.size(),
                medianCatchTime(today));
    }

    /**
     * Median catch-time across today's postings that have a known postedAt. Empty when
     * none do — honest, never faked (spec §8.4). Same method as the poll cycle uses.
     */
    private static Optional<Duration> medianCatchTime(List<Posting> today) {
        List<Duration> catches = today.stream()
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
        return Optional.of(catches.get(mid - 1).plus(catches.get(mid)).dividedBy(2));
    }

    /**
     * The agent-status snapshot. {@code lastPoll}/{@code nextPoll} are null before the
     * first-ever poll; {@code medianCatchTimeToday} is empty when no posting today has a
     * known postedAt.
     */
    public record AgentStatus(
            int boardsWatched,
            long greenhouse,
            long lever,
            long ashby,
            Instant lastPoll,
            Instant nextPoll,
            int newToday,
            Optional<Duration> medianCatchTimeToday
    ) {}
}
