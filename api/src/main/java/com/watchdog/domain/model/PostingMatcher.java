package com.watchdog.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Pure matching predicate: does a {@link Posting} satisfy a {@link FilterCriteria}?
 * Stateless, deterministic, clock-free (caller passes {@code now} for relative time).
 *
 * <p><b>Scope.</b> This evaluates only dimensions a Posting can answer on its own:
 * role/keyword (title + description), location, remote type, employment type, salary
 * floor, sponsorship signal, and time (postedWithin / seenFrom / seenTo). Dimensions
 * that live outside a single Posting — {@code seniorities} (needs the S6 title parser),
 * {@code sources} (a Company attribute), and {@code statesToShow} (a per-user
 * JobStateRecord) — are intentionally NOT evaluated here; they are resolved at the
 * query/orchestration layer in S6/S7. Keeping them out avoids a predicate that
 * silently ignores constraints.
 */
public final class PostingMatcher {

    private PostingMatcher() {
    }

    public static boolean matches(Posting posting, FilterCriteria criteria, Instant now) {
        return matchesRoleKeywords(posting, criteria.roleKeywords())
                && matchesIncludeKeywords(posting, criteria.includeKeywords())
                && matchesExcludeKeywords(posting, criteria.excludeKeywords())
                && matchesLocation(posting, criteria.locations())
                && matchesRemote(posting, criteria.remoteTypes())
                && matchesEmploymentType(posting, criteria.employmentTypes())
                && matchesSalary(posting, criteria.salaryMin(), criteria.includeUnknownSalary())
                && matchesSponsorship(posting, criteria.sponsorshipPref())
                && matchesPostedWithin(posting, criteria.postedWithin(), now)
                && matchesSeenWindow(posting, criteria.seenFrom(), criteria.seenTo());
    }

    // --- role: any keyword appears in the TITLE (OR) ---
    private static boolean matchesRoleKeywords(Posting p, List<String> roles) {
        if (roles.isEmpty()) return true;
        String title = lower(p.title());
        return roles.stream().anyMatch(r -> title.contains(lower(r)));
    }

    // --- include: every keyword appears in TITLE or DESCRIPTION (AND) ---
    private static boolean matchesIncludeKeywords(Posting p, List<String> includes) {
        if (includes.isEmpty()) return true;
        String hay = titlePlusDescription(p);
        return includes.stream().allMatch(k -> hay.contains(lower(k)));
    }

    // --- exclude: no keyword appears in TITLE or DESCRIPTION ---
    private static boolean matchesExcludeKeywords(Posting p, List<String> excludes) {
        if (excludes.isEmpty()) return true;
        String hay = titlePlusDescription(p);
        return excludes.stream().noneMatch(k -> hay.contains(lower(k)));
    }

    // --- location: any provided location is a substring of the posting's location ---
    private static boolean matchesLocation(Posting p, List<String> locations) {
        if (locations.isEmpty()) return true;
        if (p.location() == null) return false;
        String loc = lower(p.location());
        return locations.stream().anyMatch(l -> loc.contains(lower(l)));
    }

    // --- remote: posting's type is in the requested set (ANY / empty = no constraint) ---
    private static boolean matchesRemote(Posting p, java.util.Set<RemoteType> types) {
        if (types.isEmpty() || types.contains(RemoteType.ANY)) return true;
        return types.contains(p.remoteType());
    }

    private static boolean matchesEmploymentType(Posting p, java.util.Set<EmploymentType> types) {
        if (types.isEmpty()) return true;
        return types.contains(p.employmentType());
    }

    // --- salary floor: posting's max (or min) must reach salaryMin ---
    private static boolean matchesSalary(Posting p, BigDecimal salaryMin, boolean includeUnknown) {
        if (salaryMin == null) return true;
        Salary s = p.salary();
        if (s.isEmpty() || (s.max() == null && s.min() == null)) {
            return includeUnknown;
        }
        // Use the top of the band when present, else the floor.
        BigDecimal ceiling = s.max() != null ? s.max() : s.min();
        return ceiling.compareTo(salaryMin) >= 0;
    }

    private static boolean matchesSponsorship(Posting p, FilterCriteria.SponsorshipPref pref) {
        if (pref == null || pref == FilterCriteria.SponsorshipPref.ANY) return true;
        return switch (pref) {
            case REQUIRE_OFFERED -> p.sponsorshipSignal() == SponsorshipSignal.OFFERED;
            case HIDE_NOT_OFFERED -> p.sponsorshipSignal() != SponsorshipSignal.NOT_OFFERED;
            case ANY -> true;
        };
    }

    // --- postedWithin: firstSeenAt within [now - window, now]; needs a window ---
    private static boolean matchesPostedWithin(Posting p, Duration window, Instant now) {
        if (window == null) return true;
        Instant cutoff = now.minus(window);
        return !p.firstSeenAt().isBefore(cutoff);
    }

    // --- absolute seen window on firstSeenAt ---
    private static boolean matchesSeenWindow(Posting p, Instant from, Instant to) {
        Instant seen = p.firstSeenAt();
        if (from != null && seen.isBefore(from)) return false;
        return to == null || !seen.isAfter(to);
    }

    private static String titlePlusDescription(Posting p) {
        String desc = p.description() == null ? "" : p.description();
        return lower(p.title() + "\n" + desc);
    }

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
