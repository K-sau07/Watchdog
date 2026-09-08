package com.watchdog.infrastructure.web;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Seniority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PostingQueryMapperTest {

    /** All-null params → match-everything criteria. */
    private PostingQueryParams empty() {
        return new PostingQueryParams(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void emptyParamsProduceMatchAll() {
        FilterCriteria c = PostingQueryMapper.toCriteria(empty());
        assertThat(c.roleKeywords()).isEmpty();
        assertThat(c.seniorities()).isEmpty();
        assertThat(c.sources()).isEmpty();
        assertThat(c.salaryMin()).isNull();
        assertThat(c.postedWithin()).isNull();
    }

    @Test
    void splitsCommaListsAndTrims() {
        PostingQueryParams p = new PostingQueryParams(
                "new grad, swe ", "python, go", "senior", "new york, remote",
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        FilterCriteria c = PostingQueryMapper.toCriteria(p);
        assertThat(c.roleKeywords()).containsExactly("new grad", "swe");
        assertThat(c.includeKeywords()).containsExactly("python", "go");
        assertThat(c.excludeKeywords()).containsExactly("senior");
        assertThat(c.locations()).containsExactly("new york", "remote");
    }

    @Test
    void parsesEnumSetsCaseInsensitively() {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, "new_grad,JUNIOR", "remote", "full_time,internship",
                "greenhouse,LEVER", "new,saved", null, null, null, null, null, null, null, null);
        FilterCriteria c = PostingQueryMapper.toCriteria(p);
        assertThat(c.seniorities()).containsExactlyInAnyOrder(Seniority.NEW_GRAD, Seniority.JUNIOR);
        assertThat(c.remoteTypes()).containsExactly(RemoteType.REMOTE);
        assertThat(c.employmentTypes()).containsExactlyInAnyOrder(
                EmploymentType.FULL_TIME, EmploymentType.INTERNSHIP);
        assertThat(c.sources()).containsExactlyInAnyOrder(AtsSource.GREENHOUSE, AtsSource.LEVER);
        assertThat(c.statesToShow()).containsExactlyInAnyOrder(JobState.NEW, JobState.SAVED);
    }

    @Test
    void invalidEnumRaisesBadFilterParam() {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, "wizard", null, null, null, null, null, null, null,
                null, null, null, null, null);
        Throwable t = catchThrowable(() -> PostingQueryMapper.toCriteria(p));
        assertThat(t).isInstanceOf(PostingQueryMapper.BadFilterParam.class)
                .hasMessageContaining("seniority");
    }

    @Test
    void parsesSalaryAndSponsorship() {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, null, null, null, null, null,
                "130000", false, "require_offered", null, null, null, null, null);
        FilterCriteria c = PostingQueryMapper.toCriteria(p);
        assertThat(c.salaryMin()).isEqualByComparingTo(new BigDecimal("130000"));
        assertThat(c.includeUnknownSalary()).isFalse();
        assertThat(c.sponsorshipPref()).isEqualTo(FilterCriteria.SponsorshipPref.REQUIRE_OFFERED);
    }

    @Test
    void parsesPostedWithinTokens() {
        assertThat(criteriaWithPostedWithin("10m").postedWithin()).isEqualTo(Duration.ofMinutes(10));
        assertThat(criteriaWithPostedWithin("1h").postedWithin()).isEqualTo(Duration.ofHours(1));
        assertThat(criteriaWithPostedWithin("today").postedWithin()).isEqualTo(Duration.ofHours(24));
        assertThat(criteriaWithPostedWithin("week").postedWithin()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void invalidPostedWithinRaises() {
        Throwable t = catchThrowable(() -> criteriaWithPostedWithin("fortnight"));
        assertThat(t).isInstanceOf(PostingQueryMapper.BadFilterParam.class)
                .hasMessageContaining("postedWithin");
    }

    @Test
    void parsesIsoDateRange() {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                "2026-09-01T00:00:00Z", "2026-09-08T23:59:59Z", null, null);
        FilterCriteria c = PostingQueryMapper.toCriteria(p);
        assertThat(c.seenFrom()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(c.seenTo()).isEqualTo(Instant.parse("2026-09-08T23:59:59Z"));
    }

    @Test
    void invalidDateRaises() {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                "last-tuesday", null, null, null);
        Throwable t = catchThrowable(() -> PostingQueryMapper.toCriteria(p));
        assertThat(t).isInstanceOf(PostingQueryMapper.BadFilterParam.class)
                .hasMessageContaining("dateFrom");
    }

    @Test
    void pageAndSizeDefaults() {
        assertThat(empty().pageOrDefault()).isZero();
        assertThat(empty().sizeOrDefault()).isEqualTo(PostingQueryParams.DEFAULT_SIZE);

        PostingQueryParams big = new PostingQueryParams(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 3, 5000);
        assertThat(big.pageOrDefault()).isEqualTo(3);
        assertThat(big.sizeOrDefault()).isEqualTo(PostingQueryParams.MAX_SIZE); // clamped
    }

    private FilterCriteria criteriaWithPostedWithin(String token) {
        PostingQueryParams p = new PostingQueryParams(
                null, null, null, null, null, null, null, null, null, null, null, null,
                token, null, null, null, null);
        return PostingQueryMapper.toCriteria(p);
    }
}
