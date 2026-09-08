package com.watchdog.application.matching;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.PostingMatcher;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SalaryParser;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.model.SeniorityParser;
import com.watchdog.domain.model.SponsorshipParser;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.JobStateRepository;
import com.watchdog.domain.port.PostingRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The dashboard's read side (spec §5/§7): resolve a {@link FilterCriteria} into a
 * filtered, enriched, newest-first, paginated feed. Orchestration over ports — no HTTP
 * or SQL here (the web slice is S6.5, the query adapter S6.6).
 *
 * <p><b>How matching is split.</b> {@link PostingMatcher} evaluates every dimension a
 * Posting can answer alone. This service adds the three it deliberately left out
 * (documented on PostingMatcher): <i>seniority</i> (now parseable via
 * {@link SeniorityParser}), <i>source</i> (a {@link Company} attribute, resolved by a
 * companyId→source map), and <i>job state</i> (per-user, via {@link JobStateRepository};
 * a posting with no state row is implicitly {@link JobState#NEW}).
 *
 * <p><b>Enrichment (D-WD10, read-time, not persisted).</b> When the ATS gave no
 * sponsorship or salary signal, the heuristic parsers fill them in before matching, so a
 * salary-floor or sponsorship filter sees parsed values. Derived values are also exposed
 * on {@link MatchedPosting} for the API to render.
 *
 * <p><b>Fetch-then-filter (D-WD10).</b> Pull a bounded newest-first candidate window,
 * filter + enrich in memory, then sort and paginate. Pagination happens <i>after</i>
 * filtering — never push a page boundary ahead of the filters, or matches would be lost.
 */
@Service
public class MatchingService {

    /** Candidate window cap (D-WD10). Ample at v1 volume; revisit via measurement. */
    static final int CANDIDATE_LIMIT = 2000;

    private final PostingRepository postings;
    private final CompanyRepository companies;
    private final JobStateRepository jobStates;
    private final Clock clock;

    public MatchingService(PostingRepository postings, CompanyRepository companies,
                           JobStateRepository jobStates, Clock clock) {
        this.postings = postings;
        this.companies = companies;
        this.jobStates = jobStates;
        this.clock = clock;
    }

    /**
     * Filtered, enriched, newest-first, paginated feed for the given criteria.
     *
     * @param criteria the active filter (use {@link FilterCriteria#all()} for no constraint)
     * @param page     zero-based page index
     * @param size     page size (must be positive)
     */
    public Page search(FilterCriteria criteria, int page, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("page size must be positive");
        }
        Instant now = Instant.now(clock);
        Map<CompanyId, AtsSource> sourceById = companies.findActive().stream()
                .collect(Collectors.toMap(Company::id, Company::atsSource, (a, b) -> a));

        List<MatchedPosting> matched = postings.findRecent(CANDIDATE_LIMIT).stream()
                .map(this::enrich)
                .filter(m -> matchesAll(m, criteria, now, sourceById))
                .sorted(Comparator.comparing(
                        (MatchedPosting m) -> m.posting().firstSeenAt()).reversed())
                .toList();

        int total = matched.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<MatchedPosting> pageItems = matched.subList(from, to);
        return new Page(pageItems, page, size, total);
    }

    // --- enrichment: fill heuristic fields the ATS didn't provide (read-time) ---

    private MatchedPosting enrich(Posting p) {
        Seniority seniority = SeniorityParser.parse(p.title(), p.description());

        SponsorshipSignal sponsorship = p.sponsorshipSignal();
        if (sponsorship == SponsorshipSignal.UNKNOWN) {
            sponsorship = SponsorshipParser.parse(p.title(), p.description());
        }

        Salary salary = p.salary();
        if (salary.isEmpty()) {
            salary = SalaryParser.parse(p.description());
        }

        // Rebuild the posting with enriched heuristic fields so PostingMatcher sees them.
        Posting enriched = new Posting(
                p.id(), p.companyId(), p.atsPostingId(), p.title(), p.location(),
                p.remoteType(), p.department(), p.employmentType(), salary, p.url(),
                p.description(), sponsorship, p.postedAt(), p.firstSeenAt(), p.rawJson());
        return new MatchedPosting(enriched, seniority);
    }

    // --- matching: PostingMatcher + the three dimensions it leaves to the query layer ---

    private boolean matchesAll(MatchedPosting m, FilterCriteria criteria, Instant now,
                               Map<CompanyId, AtsSource> sourceById) {
        Posting p = m.posting();
        return PostingMatcher.matches(p, criteria, now)
                && matchesSeniority(m.seniority(), criteria)
                && matchesSource(p.companyId(), criteria, sourceById)
                && matchesState(p, criteria);
    }

    private static boolean matchesSeniority(Seniority seniority, FilterCriteria criteria) {
        if (criteria.seniorities().isEmpty()) return true;
        return criteria.seniorities().contains(seniority);
    }

    private static boolean matchesSource(CompanyId companyId, FilterCriteria criteria,
                                         Map<CompanyId, AtsSource> sourceById) {
        if (criteria.sources().isEmpty()) return true;
        AtsSource source = sourceById.get(companyId);
        return source != null && criteria.sources().contains(source);
    }

    /**
     * Job-state filter. A posting with no per-user state row is implicitly NEW (spec §4).
     * Empty {@code statesToShow} means no constraint; the dashboard's default view passes
     * an explicit set that omits HIDDEN.
     */
    private boolean matchesState(Posting p, FilterCriteria criteria) {
        if (criteria.statesToShow().isEmpty()) return true;
        JobState state = jobStates.findByUserAndPosting(SingleUser.ID, p.id())
                .map(r -> r.state())
                .orElse(JobState.NEW);
        return criteria.statesToShow().contains(state);
    }

    // --- result types ---

    /** A posting that passed the filter, plus the seniority derived at read time. */
    public record MatchedPosting(Posting posting, Seniority seniority) {
    }

    /** One page of matched postings (newest first) with paging metadata. */
    public record Page(List<MatchedPosting> items, int page, int size, int totalMatched) {
        public int totalPages() {
            return size == 0 ? 0 : (totalMatched + size - 1) / size;
        }
    }
}
