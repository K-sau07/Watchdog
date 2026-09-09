package com.watchdog.domain.model;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PostingMatcherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    /** A posting with sensible defaults; override via the withers below. */
    private Posting posting(String title, String description, String location,
                            RemoteType remote, EmploymentType type, Salary salary,
                            SponsorshipSignal sponsor, Instant firstSeenAt) {
        return new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-1", title,
                location, remote, null, type, salary,
                "https://x/y", description, sponsor, null, firstSeenAt, null);
    }

    private Posting swe() {
        return posting("Software Engineer, New Grad", "Join our team. Visa sponsorship available.",
                "New York, NY", RemoteType.ONSITE, EmploymentType.FULL_TIME,
                Salary.of(new BigDecimal("120000"), new BigDecimal("150000"), "USD"),
                SponsorshipSignal.OFFERED, NOW);
    }

    @Test
    void allMatchesEverything() {
        assertThat(PostingMatcher.matches(swe(), FilterCriteria.all(), NOW)).isTrue();
    }

    @Test
    void roleKeywordMatchesTitleOnly() {
        FilterCriteria c = FilterCriteria.builder().roleKeywords(List.of("new grad")).build();
        assertThat(PostingMatcher.matches(swe(), c, NOW)).isTrue();

        FilterCriteria miss = FilterCriteria.builder().roleKeywords(List.of("staff")).build();
        assertThat(PostingMatcher.matches(swe(), miss, NOW)).isFalse();
    }

    @Test
    void roleKeywordDoesNotMatchDescription() {
        // "team" is in the description, not the title — role matches title only.
        FilterCriteria c = FilterCriteria.builder().roleKeywords(List.of("team")).build();
        assertThat(PostingMatcher.matches(swe(), c, NOW)).isFalse();
    }

    @Test
    void includeKeywordsSearchTitleAndDescriptionAndAreAnded() {
        // "engineer" in title, "sponsorship" in description — both must be present.
        FilterCriteria c = FilterCriteria.builder()
                .includeKeywords(List.of("engineer", "sponsorship")).build();
        assertThat(PostingMatcher.matches(swe(), c, NOW)).isTrue();

        FilterCriteria missing = FilterCriteria.builder()
                .includeKeywords(List.of("engineer", "kubernetes")).build();
        assertThat(PostingMatcher.matches(swe(), missing, NOW)).isFalse();
    }

    @Test
    void excludeKeywordsDropOnTitleOrDescriptionHit() {
        FilterCriteria byTitle = FilterCriteria.builder().excludeKeywords(List.of("engineer")).build();
        assertThat(PostingMatcher.matches(swe(), byTitle, NOW)).isFalse();

        FilterCriteria byDesc = FilterCriteria.builder().excludeKeywords(List.of("sponsorship")).build();
        assertThat(PostingMatcher.matches(swe(), byDesc, NOW)).isFalse();

        FilterCriteria clean = FilterCriteria.builder().excludeKeywords(List.of("clearance")).build();
        assertThat(PostingMatcher.matches(swe(), clean, NOW)).isTrue();
    }

    @Test
    void locationSubstringMatch() {
        FilterCriteria c = FilterCriteria.builder().locations(List.of("new york")).build();
        assertThat(PostingMatcher.matches(swe(), c, NOW)).isTrue();

        FilterCriteria miss = FilterCriteria.builder().locations(List.of("austin")).build();
        assertThat(PostingMatcher.matches(swe(), miss, NOW)).isFalse();
    }

    @Test
    void remoteTypeConstraint() {
        FilterCriteria onsite = FilterCriteria.builder().remoteTypes(Set.of(RemoteType.ONSITE)).build();
        assertThat(PostingMatcher.matches(swe(), onsite, NOW)).isTrue();

        FilterCriteria remoteOnly = FilterCriteria.builder().remoteTypes(Set.of(RemoteType.REMOTE)).build();
        assertThat(PostingMatcher.matches(swe(), remoteOnly, NOW)).isFalse();

        FilterCriteria any = FilterCriteria.builder().remoteTypes(Set.of(RemoteType.ANY)).build();
        assertThat(PostingMatcher.matches(swe(), any, NOW)).isTrue();
    }

    @Test
    void employmentTypeConstraint() {
        FilterCriteria ft = FilterCriteria.builder()
                .employmentTypes(Set.of(EmploymentType.FULL_TIME)).build();
        assertThat(PostingMatcher.matches(swe(), ft, NOW)).isTrue();

        FilterCriteria intern = FilterCriteria.builder()
                .employmentTypes(Set.of(EmploymentType.INTERNSHIP)).build();
        assertThat(PostingMatcher.matches(swe(), intern, NOW)).isFalse();
    }

    @Test
    void salaryFloorUsesTopOfBand() {
        FilterCriteria reachable = FilterCriteria.builder().salaryMin(new BigDecimal("140000")).build();
        assertThat(PostingMatcher.matches(swe(), reachable, NOW)).isTrue(); // max 150k >= 140k

        FilterCriteria tooHigh = FilterCriteria.builder().salaryMin(new BigDecimal("160000")).build();
        assertThat(PostingMatcher.matches(swe(), tooHigh, NOW)).isFalse();
    }

    @Test
    void salaryUnknownToggle() {
        Posting noSalary = posting("SWE", "d", "NYC", RemoteType.REMOTE,
                EmploymentType.FULL_TIME, Salary.empty(), SponsorshipSignal.UNKNOWN, NOW);

        FilterCriteria include = FilterCriteria.builder()
                .salaryMin(new BigDecimal("100000")).includeUnknownSalary(true).build();
        assertThat(PostingMatcher.matches(noSalary, include, NOW)).isTrue();

        FilterCriteria exclude = FilterCriteria.builder()
                .salaryMin(new BigDecimal("100000")).includeUnknownSalary(false).build();
        assertThat(PostingMatcher.matches(noSalary, exclude, NOW)).isFalse();
    }

    @Test
    void sponsorshipPrefs() {
        Posting notOffered = posting("SWE", "US citizens only", "NYC", RemoteType.REMOTE,
                EmploymentType.FULL_TIME, Salary.empty(), SponsorshipSignal.NOT_OFFERED, NOW);
        Posting unknown = posting("SWE", "d", "NYC", RemoteType.REMOTE,
                EmploymentType.FULL_TIME, Salary.empty(), SponsorshipSignal.UNKNOWN, NOW);

        FilterCriteria require = FilterCriteria.builder()
                .sponsorshipPref(FilterCriteria.SponsorshipPref.REQUIRE_OFFERED).build();
        assertThat(PostingMatcher.matches(swe(), require, NOW)).isTrue();       // OFFERED
        assertThat(PostingMatcher.matches(unknown, require, NOW)).isFalse();    // UNKNOWN dropped

        FilterCriteria hide = FilterCriteria.builder()
                .sponsorshipPref(FilterCriteria.SponsorshipPref.HIDE_NOT_OFFERED).build();
        assertThat(PostingMatcher.matches(swe(), hide, NOW)).isTrue();          // OFFERED kept
        assertThat(PostingMatcher.matches(unknown, hide, NOW)).isTrue();        // UNKNOWN kept
        assertThat(PostingMatcher.matches(notOffered, hide, NOW)).isFalse();    // NOT_OFFERED dropped
    }

    @Test
    void postedWithinRelativeWindow() {
        // postedWithin filters on the ATS post date (postedAt), not firstSeenAt.
        Posting freshlyPosted = new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-fresh", "SWE", "NYC",
                RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "d", SponsorshipSignal.UNKNOWN,
                NOW.minus(Duration.ofMinutes(10)), NOW, null); // posted 10m ago
        Posting oldPost = new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-old", "SWE", "NYC",
                RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "d", SponsorshipSignal.UNKNOWN,
                NOW.minus(Duration.ofHours(2)), NOW, null); // posted 2h ago, seen now
        Posting unknownDate = new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-unk", "SWE", "NYC",
                RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "d", SponsorshipSignal.UNKNOWN,
                null, NOW, null); // no postedAt

        FilterCriteria within1h = FilterCriteria.builder().postedWithin(Duration.ofHours(1)).build();
        assertThat(PostingMatcher.matches(freshlyPosted, within1h, NOW)).isTrue();  // posted 10m ago
        assertThat(PostingMatcher.matches(oldPost, within1h, NOW)).isFalse();       // posted 2h ago
        assertThat(PostingMatcher.matches(unknownDate, within1h, NOW)).isFalse();   // unknown → excluded
    }

    @Test
    void absoluteSeenWindow() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T23:59:59Z");
        FilterCriteria day = FilterCriteria.builder().seenFrom(from).seenTo(to).build();
        assertThat(PostingMatcher.matches(swe(), day, NOW)).isTrue();

        Posting yesterday = posting("SWE", "d", "NYC", RemoteType.REMOTE, EmploymentType.FULL_TIME,
                Salary.empty(), SponsorshipSignal.UNKNOWN, Instant.parse("2025-12-31T12:00:00Z"));
        assertThat(PostingMatcher.matches(yesterday, day, NOW)).isFalse();
    }

    @Test
    void combinedCriteriaAllMustPass() {
        FilterCriteria c = FilterCriteria.builder()
                .roleKeywords(List.of("new grad"))
                .excludeKeywords(List.of("clearance"))
                .locations(List.of("new york"))
                .salaryMin(new BigDecimal("100000"))
                .sponsorshipPref(FilterCriteria.SponsorshipPref.HIDE_NOT_OFFERED)
                .build();
        assertThat(PostingMatcher.matches(swe(), c, NOW)).isTrue();
    }
}
