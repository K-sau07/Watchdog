package com.watchdog.infrastructure.web;

import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Seniority;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON DTOs for filter-profile CRUD (spec §7). The request/response carry the §5 filter
 * fields as plain JSON (lists of strings/enum names), converted to/from
 * {@link FilterCriteria} here so the controller stays thin. Bad enum values raise
 * {@link PostingQueryMapper.BadFilterParam} (reused) → 400.
 */
final class FilterProfileDtos {

    private FilterProfileDtos() {
    }

    /** Response body: identity + the filter dimensions. */
    record ProfileResponse(
            String id, String name,
            List<String> roleKeywords, List<String> includeKeywords, List<String> excludeKeywords,
            List<String> locations, List<String> seniorities, List<String> remoteTypes,
            List<String> employmentTypes, BigDecimal salaryMin, Boolean includeUnknownSalary,
            String sponsorship, Long postedWithinSeconds, Instant seenFrom, Instant seenTo,
            Instant createdAt, Instant updatedAt
    ) {
        static ProfileResponse from(FilterProfile p) {
            FilterCriteria c = p.criteria();
            return new ProfileResponse(
                    p.id().value().toString(), p.name(),
                    c.roleKeywords(), c.includeKeywords(), c.excludeKeywords(),
                    c.locations(), names(c.seniorities()), names(c.remoteTypes()),
                    names(c.employmentTypes()), c.salaryMin(), c.includeUnknownSalary(),
                    c.sponsorshipPref() == null ? null : c.sponsorshipPref().name(),
                    c.postedWithin() == null ? null : c.postedWithin().toSeconds(),
                    c.seenFrom(), c.seenTo(), p.createdAt(), p.updatedAt());
        }
    }

    /** Request body for PUT: name + the filter dimensions (all optional except name). */
    record ProfileUpdate(
            String name,
            List<String> roleKeywords, List<String> includeKeywords, List<String> excludeKeywords,
            List<String> locations, List<String> seniorities, List<String> remoteTypes,
            List<String> employmentTypes, BigDecimal salaryMin, Boolean includeUnknownSalary,
            String sponsorship, Long postedWithinSeconds, Instant seenFrom, Instant seenTo
    ) {
        FilterCriteria toCriteria() {
            FilterCriteria.Builder b = FilterCriteria.builder()
                    .roleKeywords(orEmpty(roleKeywords))
                    .includeKeywords(orEmpty(includeKeywords))
                    .excludeKeywords(orEmpty(excludeKeywords))
                    .locations(orEmpty(locations))
                    .seniorities(enumSet(seniorities, Seniority.class, "seniorities"))
                    .remoteTypes(enumSet(remoteTypes, RemoteType.class, "remoteTypes"))
                    .employmentTypes(enumSet(employmentTypes, EmploymentType.class, "employmentTypes"))
                    .salaryMin(salaryMin)
                    .seenFrom(seenFrom)
                    .seenTo(seenTo);
            if (includeUnknownSalary != null) b.includeUnknownSalary(includeUnknownSalary);
            if (sponsorship != null && !sponsorship.isBlank()) {
                b.sponsorshipPref(parseEnum(sponsorship, FilterCriteria.SponsorshipPref.class, "sponsorship"));
            }
            if (postedWithinSeconds != null) b.postedWithin(Duration.ofSeconds(postedWithinSeconds));
            return b.build();
        }
    }

    // --- helpers ---

    private static <E extends Enum<E>> List<String> names(Set<E> set) {
        return set.stream().map(Enum::name).collect(Collectors.toList());
    }

    private static List<String> orEmpty(List<String> v) {
        return v == null ? List.of() : v;
    }

    private static <E extends Enum<E>> Set<E> enumSet(List<String> raw, Class<E> type, String field) {
        if (raw == null || raw.isEmpty()) return Set.of();
        return raw.stream().map(s -> parseEnum(s, type, field)).collect(Collectors.toSet());
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String field) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PostingQueryMapper.BadFilterParam("invalid " + field + " value: '" + raw + "'");
        }
    }
}
