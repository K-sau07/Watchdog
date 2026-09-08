package com.watchdog.persistence;

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
import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PostingRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    PostingRepository postings;
    @Autowired
    CompanyRepository companies;

    private CompanyId newCompany(String slug) {
        Company c = Company.register("Co-" + slug, AtsSource.GREENHOUSE, slug);
        companies.save(c);
        return c.id();
    }

    private Posting posting(CompanyId companyId, String atsId, Salary salary, String raw) {
        return new Posting(
                PostingId.generate(), companyId, atsId, "Software Engineer, New Grad",
                "New York, NY", RemoteType.HYBRID, "Engineering", EmploymentType.FULL_TIME,
                salary, "https://x/y", "Great role. Visa sponsorship available.",
                SponsorshipSignal.OFFERED,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                Instant.now().truncatedTo(ChronoUnit.MILLIS), raw);
    }

    private Posting postingSeenAt(CompanyId companyId, String atsId, Instant firstSeenAt) {
        return new Posting(
                PostingId.generate(), companyId, atsId, "Software Engineer, New Grad",
                "New York, NY", RemoteType.HYBRID, "Engineering", EmploymentType.FULL_TIME,
                Salary.empty(), "https://x/y", "desc", SponsorshipSignal.UNKNOWN,
                null, firstSeenAt.truncatedTo(ChronoUnit.MILLIS), null);
    }

    @Test
    void saveAndFindByIdRoundTripsSalaryAndJsonb() {
        CompanyId co = newCompany("rt-" + System.nanoTime());
        Salary salary = Salary.of(new BigDecimal("120000"), new BigDecimal("150000"), "USD");
        Posting p = posting(co, "gh-1", salary, "{\"team\":\"platform\",\"level\":\"L3\"}");
        postings.save(p);

        assertThat(postings.findById(p.id())).isPresent().get().satisfies(found -> {
            assertThat(found.title()).contains("New Grad");
            assertThat(found.salary().min()).isEqualByComparingTo("120000");
            assertThat(found.salary().max()).isEqualByComparingTo("150000");
            assertThat(found.salary().currency()).isEqualTo("USD");
            assertThat(found.sponsorshipSignal()).isEqualTo(SponsorshipSignal.OFFERED);
            assertThat(found.rawJson()).contains("platform");
        });
    }

    @Test
    void emptySalaryAndNullRawHandled() {
        CompanyId co = newCompany("empty-" + System.nanoTime());
        Posting p = posting(co, "gh-empty", Salary.empty(), null);
        postings.save(p);

        assertThat(postings.findById(p.id())).isPresent().get().satisfies(found -> {
            assertThat(found.salary().isEmpty()).isTrue();
            assertThat(found.rawJson()).isNull();
        });
    }

    @Test
    void dedupByNaturalKey() {
        CompanyId co = newCompany("dedup-" + System.nanoTime());
        Posting p = posting(co, "gh-dedup-1", Salary.empty(), null);
        postings.save(p);

        assertThat(postings.existsByNaturalKey(co, "gh-dedup-1")).isTrue();
        assertThat(postings.existsByNaturalKey(co, "gh-does-not-exist")).isFalse();
        assertThat(postings.findByNaturalKey(co, "gh-dedup-1")).isPresent();
    }

    @Test
    void findByCompanyReturnsOnlyThatCompanysPostings() {
        CompanyId a = newCompany("a-" + System.nanoTime());
        CompanyId b = newCompany("b-" + System.nanoTime());
        postings.save(posting(a, "a-1", Salary.empty(), null));
        postings.save(posting(a, "a-2", Salary.empty(), null));
        postings.save(posting(b, "b-1", Salary.empty(), null));

        assertThat(postings.findByCompany(a)).hasSize(2);
        assertThat(postings.findByCompany(b)).hasSize(1);
    }

    @Test
    void findRecentOrdersByFirstSeenDescending() {
        // Distinct, far-apart timestamps so ordering is unambiguous even with other rows
        // present in the shared container.
        CompanyId co = newCompany("recent-" + System.nanoTime());
        Instant base = Instant.parse("2099-01-01T00:00:00Z"); // future: sorts above other tests' rows
        Posting oldest = postingSeenAt(co, "r-old", base);
        Posting middle = postingSeenAt(co, "r-mid", base.plusSeconds(60));
        Posting newest = postingSeenAt(co, "r-new", base.plusSeconds(120));
        // insert out of order to prove the query sorts, not insertion order
        postings.save(middle);
        postings.save(oldest);
        postings.save(newest);

        var ids = postings.findRecent(1000).stream().map(Posting::id).toList();
        // our three, in newest-first order, appear as a subsequence
        int iNew = ids.indexOf(newest.id());
        int iMid = ids.indexOf(middle.id());
        int iOld = ids.indexOf(oldest.id());
        assertThat(iNew).isGreaterThanOrEqualTo(0);
        assertThat(iNew).isLessThan(iMid);
        assertThat(iMid).isLessThan(iOld);
    }

    @Test
    void findRecentHonorsLimit() {
        CompanyId co = newCompany("limit-" + System.nanoTime());
        for (int i = 0; i < 5; i++) {
            postings.save(posting(co, "lim-" + i, Salary.empty(), null));
        }
        // the table now has >= 3 rows; the limit caps the result size exactly
        assertThat(postings.findRecent(3)).hasSize(3);
    }
}
