package com.watchdog.application.polling;

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
import com.watchdog.domain.port.JobSourcePort;
import com.watchdog.domain.port.PollingUseCase;
import com.watchdog.domain.port.PostingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PollingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    // --- in-memory fakes ---

    private static class FakeCompanyRepo implements CompanyRepository {
        final Map<CompanyId, Company> store = new HashMap<>();
        final AtomicInteger saveCount = new AtomicInteger();
        @Override public Company save(Company c) { store.put(c.id(), c); saveCount.incrementAndGet(); return c; }
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Company> findBySourceAndSlug(AtsSource s, String slug) { return Optional.empty(); }
        @Override public List<Company> findActive() { return new ArrayList<>(store.values()); }
        @Override public long countBySource(AtsSource s) { return 0; }
    }

    private static class FakePostingRepo implements PostingRepository {
        final List<Posting> saved = new ArrayList<>();
        @Override public Posting save(Posting p) { saved.add(p); return p; }
        @Override public Optional<Posting> findById(PostingId id) { return Optional.empty(); }
        @Override public Optional<Posting> findByNaturalKey(CompanyId c, String a) {
            return saved.stream().filter(p -> p.companyId().equals(c) && p.atsPostingId().equals(a)).findFirst();
        }
        @Override public boolean existsByNaturalKey(CompanyId c, String a) {
            return findByNaturalKey(c, a).isPresent();
        }
        @Override public List<Posting> findByCompany(CompanyId c) { return List.of(); }
        @Override public List<Posting> findRecent(int limit) {
            return saved.stream().limit(limit).toList();
        }
        @Override public List<Posting> findSeenSince(Instant since) {
            return saved.stream().filter(p -> !p.firstSeenAt().isBefore(since)).toList();
        }
        @Override public int deleteStalePostedBefore(Instant cutoff) { return 0; }
    }

    private Posting posting(CompanyId companyId, String atsId, Instant postedAt) {
        return new Posting(PostingId.generate(), companyId, atsId, "SWE", "NYC",
                RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "desc", SponsorshipSignal.UNKNOWN, postedAt, NOW, null);
    }

    private JobSourcePort portReturning(AtsSource source, List<Posting> postings) {
        return new JobSourcePort() {
            @Override public AtsSource source() { return source; }
            @Override public List<Posting> fetchPostings(Company c) { return postings; }
        };
    }

    private JobSourcePort portThatFails(AtsSource source) {
        return new JobSourcePort() {
            @Override public AtsSource source() { return source; }
            @Override public List<Posting> fetchPostings(Company c) {
                throw new RuntimeException("boom");
            }
        };
    }

    @Test
    void persistsNewPostings() {
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company co = Company.register("Co", AtsSource.GREENHOUSE, "co");
        companies.save(co);

        var port = portReturning(AtsSource.GREENHOUSE,
                List.of(posting(co.id(), "a1", NOW.minus(Duration.ofMinutes(3))),
                        posting(co.id(), "a2", NOW.minus(Duration.ofMinutes(5)))));
        var service = new PollingService(new AtsSourceRouter(List.of(port)), companies, postings, clock, 0, 0);

        PollingUseCase.PollCycleResult r = service.runOnce();
        assertThat(r.newPostings()).isEqualTo(2);
        assertThat(r.companiesPolled()).isEqualTo(1);
        assertThat(r.companiesFailed()).isZero();
        assertThat(postings.saved).hasSize(2);
    }

    @Test
    void dedupsAlreadySeenPostings() {
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company co = Company.register("Co", AtsSource.GREENHOUSE, "co");
        companies.save(co);
        // pre-seed one posting as already seen
        postings.save(posting(co.id(), "a1", NOW));

        var port = portReturning(AtsSource.GREENHOUSE,
                List.of(posting(co.id(), "a1", NOW),   // dup
                        posting(co.id(), "a2", NOW)));  // new
        var service = new PollingService(new AtsSourceRouter(List.of(port)), companies, postings, clock, 0, 0);

        PollingUseCase.PollCycleResult r = service.runOnce();
        assertThat(r.newPostings()).isEqualTo(1); // only a2
    }

    @Test
    void oneFailingSourceDoesNotAbortCycle() {
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company good = Company.register("Good", AtsSource.GREENHOUSE, "good");
        Company bad = Company.register("Bad", AtsSource.LEVER, "bad");
        companies.save(good);
        companies.save(bad);

        var ghPort = portReturning(AtsSource.GREENHOUSE, List.of(posting(good.id(), "g1", NOW)));
        var leverPort = portThatFails(AtsSource.LEVER);
        var service = new PollingService(
                new AtsSourceRouter(List.of(ghPort, leverPort)), companies, postings, clock, 0, 0);

        PollingUseCase.PollCycleResult r = service.runOnce();
        assertThat(r.companiesPolled()).isEqualTo(1);
        assertThat(r.companiesFailed()).isEqualTo(1);
        assertThat(r.newPostings()).isEqualTo(1); // the good one still went through
    }

    @Test
    void medianCatchTimeComputedFromKnownPostedAt() {
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company co = Company.register("Co", AtsSource.GREENHOUSE, "co");
        companies.save(co);
        // catch times: 2, 4, 6 min -> median 4
        var port = portReturning(AtsSource.GREENHOUSE, List.of(
                posting(co.id(), "a1", NOW.minus(Duration.ofMinutes(2))),
                posting(co.id(), "a2", NOW.minus(Duration.ofMinutes(4))),
                posting(co.id(), "a3", NOW.minus(Duration.ofMinutes(6)))));
        var service = new PollingService(new AtsSourceRouter(List.of(port)), companies, postings, clock, 0, 0);

        PollingUseCase.PollCycleResult r = service.runOnce();
        assertThat(r.medianCatchTime()).contains(Duration.ofMinutes(4));
    }

    @Test
    void medianEmptyWhenNoPostedAt() {
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company co = Company.register("Co", AtsSource.GREENHOUSE, "co");
        companies.save(co);
        var port = portReturning(AtsSource.GREENHOUSE,
                List.of(posting(co.id(), "a1", null))); // no postedAt
        var service = new PollingService(new AtsSourceRouter(List.of(port)), companies, postings, clock, 0, 0);

        assertThat(service.runOnce().medianCatchTime()).isEmpty();
    }

    @Test
    void noActiveCompaniesIsANoOp() {
        var service = new PollingService(
                new AtsSourceRouter(List.of()), new FakeCompanyRepo(), new FakePostingRepo(), clock, 0, 0);
        PollingUseCase.PollCycleResult r = service.runOnce();
        assertThat(r.companiesPolled()).isZero();
        assertThat(r.newPostings()).isZero();
    }

    @Test
    void staggeredRunStillPollsEveryCompany() {
        // Stagger must not drop or skip any board — same result as an un-staggered run.
        FakeCompanyRepo companies = new FakeCompanyRepo();
        FakePostingRepo postings = new FakePostingRepo();
        Company a = Company.register("A", AtsSource.GREENHOUSE, "a");
        Company b = Company.register("B", AtsSource.GREENHOUSE, "b");
        companies.save(a);
        companies.save(b);
        var port = portReturning(AtsSource.GREENHOUSE,
                List.of(posting(a.id(), "a1", NOW), posting(b.id(), "b1", NOW)));
        // tiny 1ms jitter so the test stays fast but the stagger path executes
        var service = new PollingService(new AtsSourceRouter(List.of(port)), companies, postings, clock, 1, 1);

        PollingUseCase.PollCycleResult r = service.runStaggered();
        assertThat(r.companiesPolled()).isEqualTo(2);
        assertThat(r.companiesFailed()).isZero();
    }
}
