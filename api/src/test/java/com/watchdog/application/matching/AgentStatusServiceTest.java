package com.watchdog.application.matching;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.PostingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgentStatusServiceTest {

    // Noon UTC so "start of day" is a clean 00:00 the same date.
    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private static final long INTERVAL_MS = 120_000; // 2 min (D-WD2)
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FakeCompanyRepo companies = new FakeCompanyRepo();
    private final FakePostingRepo postings = new FakePostingRepo();

    private AgentStatusService service() {
        return new AgentStatusService(companies, postings, clock, INTERVAL_MS);
    }

    private static class FakeCompanyRepo implements CompanyRepository {
        final List<Company> store = new ArrayList<>();
        @Override public Company save(Company c) { store.add(c); return c; }
        @Override public Optional<Company> findById(CompanyId id) { return Optional.empty(); }
        @Override public Optional<Company> findBySourceAndSlug(AtsSource s, String slug) { return Optional.empty(); }
        @Override public List<Company> findActive() { return store; }
        @Override public long countBySource(AtsSource s) { return 0; }
    }

    private static class FakePostingRepo implements PostingRepository {
        final List<Posting> store = new ArrayList<>();
        @Override public Posting save(Posting p) { store.add(p); return p; }
        @Override public Optional<Posting> findById(PostingId id) { return Optional.empty(); }
        @Override public Optional<Posting> findByNaturalKey(CompanyId c, String a) { return Optional.empty(); }
        @Override public boolean existsByNaturalKey(CompanyId c, String a) { return false; }
        @Override public List<Posting> findByCompany(CompanyId c) { return List.of(); }
        @Override public List<Posting> findRecent(int limit) { return List.of(); }
        @Override public List<Posting> findSeenSince(Instant since) {
            return store.stream().filter(p -> !p.firstSeenAt().isBefore(since)).toList();
        }
        @Override public int deleteStalePostedBefore(Instant cutoff) { return 0; }
    }

    private Company companyPolledAt(AtsSource source, Instant polledAt) {
        Company c = Company.register("Co-" + source + System.nanoTime(), source, "slug" + System.nanoTime());
        c = polledAt == null ? c : c.polledAt(polledAt);
        companies.save(c);
        return c;
    }

    private void postingSeenAt(Instant seenAt, Instant postedAt) {
        postings.save(new Posting(PostingId.generate(), CompanyId.generate(), "ats-" + System.nanoTime(),
                "SWE", "NYC", RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "desc", SponsorshipSignal.UNKNOWN, postedAt, seenAt, null));
    }

    @Test
    void countsBoardsPerSource() {
        companyPolledAt(AtsSource.GREENHOUSE, NOW);
        companyPolledAt(AtsSource.GREENHOUSE, NOW);
        companyPolledAt(AtsSource.ASHBY, NOW);

        AgentStatusService.AgentStatus s = service().current();
        assertThat(s.boardsWatched()).isEqualTo(3);
        assertThat(s.greenhouse()).isEqualTo(2);
        assertThat(s.ashby()).isEqualTo(1);
        assertThat(s.lever()).isZero();
    }

    @Test
    void lastPollIsMostRecentAndNextIsPlusInterval() {
        companyPolledAt(AtsSource.GREENHOUSE, NOW.minus(Duration.ofMinutes(5)));
        companyPolledAt(AtsSource.ASHBY, NOW.minus(Duration.ofMinutes(1))); // most recent

        AgentStatusService.AgentStatus s = service().current();
        assertThat(s.lastPoll()).isEqualTo(NOW.minus(Duration.ofMinutes(1)));
        assertThat(s.nextPoll()).isEqualTo(NOW.minus(Duration.ofMinutes(1)).plusMillis(INTERVAL_MS));
    }

    @Test
    void pollTimesAreNullBeforeFirstPoll() {
        companyPolledAt(AtsSource.GREENHOUSE, null); // never polled

        AgentStatusService.AgentStatus s = service().current();
        assertThat(s.lastPoll()).isNull();
        assertThat(s.nextPoll()).isNull();
    }

    @Test
    void newTodayCountsOnlyTodaysPostings() {
        postingSeenAt(NOW, NOW.minus(Duration.ofMinutes(3)));                 // today
        postingSeenAt(NOW.minus(Duration.ofHours(2)), null);                  // today (earlier)
        postingSeenAt(Instant.parse("2026-09-07T12:00:00Z"), null);          // yesterday

        assertThat(service().current().newToday()).isEqualTo(2);
    }

    @Test
    void medianCatchTimeTodayFromKnownPostedAt() {
        // catch times 2, 4, 6 min → median 4; a no-postedAt row is ignored, not faked.
        postingSeenAt(NOW, NOW.minus(Duration.ofMinutes(2)));
        postingSeenAt(NOW, NOW.minus(Duration.ofMinutes(4)));
        postingSeenAt(NOW, NOW.minus(Duration.ofMinutes(6)));
        postingSeenAt(NOW, null);

        assertThat(service().current().medianCatchTimeToday()).contains(Duration.ofMinutes(4));
    }

    @Test
    void medianEmptyWhenNoKnownPostedAtToday() {
        postingSeenAt(NOW, null);
        assertThat(service().current().medianCatchTimeToday()).isEmpty();
    }

    @Test
    void emptyRegistryIsAllZeroNoNulls() {
        AgentStatusService.AgentStatus s = service().current();
        assertThat(s.boardsWatched()).isZero();
        assertThat(s.newToday()).isZero();
        assertThat(s.lastPoll()).isNull();
        assertThat(s.medianCatchTimeToday()).isEmpty();
    }
}
