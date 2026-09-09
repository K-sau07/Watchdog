package com.watchdog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * The user's filter criteria across every §5 dimension. A value object: immutable,
 * with a builder for ergonomics. Empty/absent fields mean "no constraint on this
 * dimension" — so {@link #all()} matches everything.
 *
 * <p>The actual matching lives in {@link PostingMatcher} (a pure predicate). Heuristic
 * parsers that populate a Posting's seniority/sponsorship/etc. are out of scope here —
 * they arrive in S6. This type only constrains fields already present on a Posting.
 *
 * <p>Time filtering is explicit and clock-free: {@code postedWithin} is a relative
 * window evaluated against a caller-supplied "now"; {@code seenFrom}/{@code seenTo}
 * bound {@code firstSeenAt} absolutely (spec §5 dates).
 */
public record FilterCriteria(
        List<String> roleKeywords,        // any-match against title (OR)
        List<String> includeKeywords,     // all-match against title+description (AND)
        List<String> excludeKeywords,     // none-match against title+description
        Set<Seniority> seniorities,       // empty = any
        Set<RemoteType> remoteTypes,      // empty/ANY = any
        List<String> locations,           // any-match substring against location
        Set<EmploymentType> employmentTypes, // empty = any
        BigDecimal salaryMin,             // null = no floor
        boolean includeUnknownSalary,     // when salaryMin set: keep postings w/o salary?
        SponsorshipPref sponsorshipPref,  // ANY = no constraint
        Set<AtsSource> sources,           // empty = any
        Set<JobState> statesToShow,       // empty = any (feed usually hides HIDDEN)
        java.time.Duration postedWithin,  // null = no relative window
        Instant seenFrom,                 // null = no lower bound
        Instant seenTo,                   // null = no upper bound
        boolean usOnly                    // true = keep only US locations (D-WD17, inclusive)
) {

    /** Sponsorship filter preference (distinct from a posting's SponsorshipSignal). */
    public enum SponsorshipPref {
        REQUIRE_OFFERED,   // keep only OFFERED
        HIDE_NOT_OFFERED,  // drop NOT_OFFERED (keep OFFERED + UNKNOWN)
        ANY                // no constraint
    }

    /** Match-everything default (an empty filter). */
    public static FilterCriteria all() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Ergonomic builder; every field defaults to "no constraint". */
    public static final class Builder {
        private List<String> roleKeywords = List.of();
        private List<String> includeKeywords = List.of();
        private List<String> excludeKeywords = List.of();
        private Set<Seniority> seniorities = Set.of();
        private Set<RemoteType> remoteTypes = Set.of();
        private List<String> locations = List.of();
        private Set<EmploymentType> employmentTypes = Set.of();
        private BigDecimal salaryMin = null;
        private boolean includeUnknownSalary = true;
        private SponsorshipPref sponsorshipPref = SponsorshipPref.ANY;
        private Set<AtsSource> sources = Set.of();
        private Set<JobState> statesToShow = Set.of();
        private java.time.Duration postedWithin = null;
        private Instant seenFrom = null;
        private Instant seenTo = null;
        private boolean usOnly = false;

        public Builder roleKeywords(List<String> v) { this.roleKeywords = safe(v); return this; }
        public Builder includeKeywords(List<String> v) { this.includeKeywords = safe(v); return this; }
        public Builder excludeKeywords(List<String> v) { this.excludeKeywords = safe(v); return this; }
        public Builder seniorities(Set<Seniority> v) { this.seniorities = safeSet(v); return this; }
        public Builder remoteTypes(Set<RemoteType> v) { this.remoteTypes = safeSet(v); return this; }
        public Builder locations(List<String> v) { this.locations = safe(v); return this; }
        public Builder employmentTypes(Set<EmploymentType> v) { this.employmentTypes = safeSet(v); return this; }
        public Builder salaryMin(BigDecimal v) { this.salaryMin = v; return this; }
        public Builder includeUnknownSalary(boolean v) { this.includeUnknownSalary = v; return this; }
        public Builder sponsorshipPref(SponsorshipPref v) { this.sponsorshipPref = v == null ? SponsorshipPref.ANY : v; return this; }
        public Builder sources(Set<AtsSource> v) { this.sources = safeSet(v); return this; }
        public Builder statesToShow(Set<JobState> v) { this.statesToShow = safeSet(v); return this; }
        public Builder postedWithin(java.time.Duration v) { this.postedWithin = v; return this; }
        public Builder seenFrom(Instant v) { this.seenFrom = v; return this; }
        public Builder seenTo(Instant v) { this.seenTo = v; return this; }
        public Builder usOnly(boolean v) { this.usOnly = v; return this; }

        public FilterCriteria build() {
            return new FilterCriteria(
                    roleKeywords, includeKeywords, excludeKeywords, seniorities, remoteTypes,
                    locations, employmentTypes, salaryMin, includeUnknownSalary, sponsorshipPref,
                    sources, statesToShow, postedWithin, seenFrom, seenTo, usOnly);
        }

        private static List<String> safe(List<String> v) { return v == null ? List.of() : List.copyOf(v); }
        private static <T> Set<T> safeSet(Set<T> v) { return v == null ? Set.of() : Set.copyOf(v); }
    }
}
