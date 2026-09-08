package com.watchdog.application.matching;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.JobStateRepository;
import com.watchdog.domain.port.PostingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    // --- in-memory fakes ---

    private static class FakePostingRepo implements PostingRepository {
        final List<Posting> store = new ArrayList<>();
        @Override public Posting save(Posting p) { store.add(p); return p; }
        @Override public Optional<Posting> findById(PostingId id) {
            return store.stream().filter(p -> p.id().equals(id)).findFirst();
        }
        @Override public Optional<Posting> findByNaturalKey(CompanyId c, String a) { return Optional.empty(); }
        @Override public boolean existsByNaturalKey(CompanyId c, String a) { return false; }
        @Override public List<Posting> findByCompany(CompanyId c) { return List.of(); }
        @Override public List<Posting> findRecent(int limit) {
            // Emulate the SQL contract: newest-first, capped.
            return store.stream()
                    .sorted((a, b) -> b.firstSeenAt().compareTo(a.firstSeenAt()))
                    .limit(limit).toList();
        }
        @Override public List<Posting> findSeenSince(java.time.Instant since) {
            return store.stream().filter(p -> !p.firstSeenAt().isBefore(since)).toList();
        }
    }

    private static class FakeCompanyRepo implements CompanyRepository {
        final Map<CompanyId, Company> store = new HashMap<>();
        @Override public Company save(Company c) { store.put(c.id(), c); return c; }
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Company> findBySourceAndSlug(AtsSource s, String slug) { return Optional.empty(); }
        @Override public List<Company> findActive() { return new ArrayList<>(store.values()); }
        @Override public long countBySource(AtsSource s) { return 0; }
    }

    private static class FakeJobStateRepo implements JobStateRepository {
        final Map<PostingId, JobStateRecord> byPosting = new HashMap<>();
        @Override public JobStateRecord save(JobStateRecord r) { byPosting.put(r.postingId(), r); return r; }
        @Override public Optional<JobStateRecord> findByUserAndPosting(UserId u, PostingId p) {
            return Optional.ofNullable(byPosting.get(p));
        }
    }

    // --- builders ---

    private final FakePostingRepo postings = new FakePostingRepo();
    private final FakeCompanyRepo companies = new FakeCompanyRepo();
    private final FakeJobStateRepo jobStates = new FakeJobStateRepo();

    private MatchingService service() {
        return new MatchingService(postings, companies, jobStates, clock);
    }

    private Company company(AtsSource source) {
        Company c = Company.register("Co-" + source, source, "slug-" + source);
        companies.save(c);
        return c;
    }

    private Posting posting(Company co, String title, String description, Instant seenAt,
                            Salary salary, SponsorshipSignal sponsorship) {
        Posting p = new Posting(PostingId.generate(), co.id(), "ats-" + title.hashCode() + seenAt,
                title, "NYC", RemoteType.REMOTE, null, EmploymentType.FULL_TIME, salary,
                "https://x/y", description, sponsorship, seenAt.minus(Duration.ofMinutes(3)),
                seenAt, null);
        postings.save(p);
        return p;
    }

    // --- tests ---

    @Test
    void allCriteriaReturnsEverythingNewestFirst() {
        Company gh = company(AtsSource.GREENHOUSE);
        posting(gh, "SWE A", "desc", NOW.minus(Duration.ofMinutes(10)), Salary.empty(), SponsorshipSignal.UNKNOWN);
        posting(gh, "SWE B", "desc", NOW.minus(Duration.ofMinutes(2)), Salary.empty(), SponsorshipSignal.UNKNOWN);

        MatchingService.Page page = service().search(FilterCriteria.all(), 0, 10);

        assertThat(page.totalMatched()).isEqualTo(2);
        // newest first: B (2 min ago) before A (10 min ago)
        assertThat(page.items().get(0).posting().title()).isEqualTo("SWE B");
        assertThat(page.items().get(1).posting().title()).isEqualTo("SWE A");
    }

    @Test
    void filtersBySeniorityUsingTheParser() {
        Company gh = company(AtsSource.GREENHOUSE);
        posting(gh, "Software Engineer, New Grad", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);
        posting(gh, "Senior Software Engineer", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);

        FilterCriteria newGradOnly = FilterCriteria.builder()
                .seniorities(Set.of(Seniority.NEW_GRAD)).build();
        MatchingService.Page page = service().search(newGradOnly, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("Software Engineer, New Grad");
        assertThat(page.items().get(0).seniority()).isEqualTo(Seniority.NEW_GRAD);
    }

    @Test
    void filtersBySource() {
        Company gh = company(AtsSource.GREENHOUSE);
        Company lever = company(AtsSource.LEVER);
        posting(gh, "SWE GH", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);
        posting(lever, "SWE Lever", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);

        FilterCriteria leverOnly = FilterCriteria.builder()
                .sources(Set.of(AtsSource.LEVER)).build();
        MatchingService.Page page = service().search(leverOnly, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("SWE Lever");
    }

    @Test
    void filtersByJobStateAndTreatsNoRowAsNew() {
        Company gh = company(AtsSource.GREENHOUSE);
        Posting shown = posting(gh, "SWE New", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);
        Posting hidden = posting(gh, "SWE Hidden", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);
        // mark the second HIDDEN for the single user
        jobStates.save(JobStateRecord.initial(SingleUser.ID, hidden.id(), NOW)
                .transitionTo(JobState.HIDDEN, NOW));

        // default-view style: show everything except HIDDEN
        FilterCriteria notHidden = FilterCriteria.builder()
                .statesToShow(Set.of(JobState.NEW, JobState.SAVED, JobState.APPLIED)).build();
        MatchingService.Page page = service().search(notHidden, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("SWE New");
    }

    @Test
    void enrichesSponsorshipFromDescriptionThenFilters() {
        Company gh = company(AtsSource.GREENHOUSE);
        // ATS gave UNKNOWN; the body says citizens-only -> parser -> NOT_OFFERED
        posting(gh, "SWE", "Applicants must be a US citizen.", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);
        // ATS gave UNKNOWN; body offers sponsorship -> OFFERED
        posting(gh, "SWE Sponsor", "We will sponsor visas.", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);

        FilterCriteria requireSponsorship = FilterCriteria.builder()
                .sponsorshipPref(FilterCriteria.SponsorshipPref.REQUIRE_OFFERED).build();
        MatchingService.Page page = service().search(requireSponsorship, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("SWE Sponsor");
    }

    @Test
    void enrichesSalaryFromDescriptionThenAppliesFloor() {
        Company gh = company(AtsSource.GREENHOUSE);
        // ATS gave no salary; body has a range -> parsed, passes a 130k floor on top-of-band
        posting(gh, "SWE Paid", "Comp: $120,000 - $150,000 per year.", NOW,
                Salary.empty(), SponsorshipSignal.UNKNOWN);
        // ATS gave no salary; body has none -> stays empty -> excluded when includeUnknown=false
        posting(gh, "SWE Unpaid", "Join our team.", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);

        FilterCriteria floor = FilterCriteria.builder()
                .salaryMin(new BigDecimal("130000")).includeUnknownSalary(false).build();
        MatchingService.Page page = service().search(floor, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("SWE Paid");
    }

    @Test
    void paginatesAfterFiltering() {
        Company gh = company(AtsSource.GREENHOUSE);
        // 5 matching postings, staggered so order is deterministic (newest first)
        for (int i = 0; i < 5; i++) {
            posting(gh, "SWE " + i, "d", NOW.minus(Duration.ofMinutes(i)),
                    Salary.empty(), SponsorshipSignal.UNKNOWN);
        }
        MatchingService svc = service();

        MatchingService.Page p0 = svc.search(FilterCriteria.all(), 0, 2);
        assertThat(p0.items()).hasSize(2);
        assertThat(p0.totalMatched()).isEqualTo(5);
        assertThat(p0.totalPages()).isEqualTo(3);
        assertThat(p0.items().get(0).posting().title()).isEqualTo("SWE 0"); // newest

        MatchingService.Page p2 = svc.search(FilterCriteria.all(), 2, 2);
        assertThat(p2.items()).hasSize(1); // last page has the remainder
        assertThat(p2.items().get(0).posting().title()).isEqualTo("SWE 4"); // oldest
    }

    @Test
    void pageBeyondEndIsEmptyNotError() {
        Company gh = company(AtsSource.GREENHOUSE);
        posting(gh, "SWE", "d", NOW, Salary.empty(), SponsorshipSignal.UNKNOWN);

        MatchingService.Page page = service().search(FilterCriteria.all(), 5, 10);
        assertThat(page.items()).isEmpty();
        assertThat(page.totalMatched()).isEqualTo(1);
    }

    @Test
    void combinesKeywordSeniorityAndSource() {
        Company gh = company(AtsSource.GREENHOUSE);
        Company lever = company(AtsSource.LEVER);
        posting(gh, "New Grad Software Engineer", "python backend role", NOW,
                Salary.empty(), SponsorshipSignal.UNKNOWN);      // matches all
        posting(gh, "New Grad Data Analyst", "python role", NOW,
                Salary.empty(), SponsorshipSignal.UNKNOWN);      // wrong role keyword
        posting(lever, "New Grad Software Engineer", "python", NOW,
                Salary.empty(), SponsorshipSignal.UNKNOWN);      // wrong source

        FilterCriteria c = FilterCriteria.builder()
                .roleKeywords(List.of("software engineer"))
                .seniorities(Set.of(Seniority.NEW_GRAD))
                .sources(Set.of(AtsSource.GREENHOUSE))
                .build();
        MatchingService.Page page = service().search(c, 0, 10);

        assertThat(page.totalMatched()).isEqualTo(1);
        assertThat(page.items().get(0).posting().title()).isEqualTo("New Grad Software Engineer");
    }

    @Test
    void rejectsNonPositivePageSize() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service().search(FilterCriteria.all(), 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
