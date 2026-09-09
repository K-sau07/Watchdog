package com.watchdog.infrastructure.web;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Seniority;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps raw {@code /api/postings} query parameters to a domain {@link FilterCriteria}
 * (spec §5/§7). Pure and static so it is unit-testable without Spring. The controller
 * stays thin — parse here, match in the service.
 *
 * <p>List params are comma-separated (trimmed, blanks dropped). Enum params are
 * case-insensitive. Unknown enum values, bad numbers/dates, or an unrecognized
 * {@code postedWithin} token raise {@link BadFilterParam}, which the controller renders
 * as HTTP 400 — a malformed filter is a client error, never a silent no-op.
 */
final class PostingQueryMapper {

    private PostingQueryMapper() {
    }

    /** Raised when a query parameter can't be parsed; becomes a 400 at the controller. */
    static final class BadFilterParam extends RuntimeException {
        BadFilterParam(String message) {
            super(message);
        }
    }

    static FilterCriteria toCriteria(PostingQueryParams p) {
        FilterCriteria.Builder b = FilterCriteria.builder()
                .roleKeywords(splitList(p.roles()))
                .includeKeywords(splitList(p.keywords()))
                .excludeKeywords(splitList(p.exclude()))
                .locations(splitList(p.location()))
                .seniorities(parseEnumSet(p.seniority(), Seniority.class, "seniority"))
                .remoteTypes(parseEnumSet(p.remote(), RemoteType.class, "remote"))
                .employmentTypes(parseEnumSet(p.employmentType(), EmploymentType.class, "employmentType"))
                .sources(parseEnumSet(p.source(), AtsSource.class, "source"))
                .statesToShow(parseEnumSet(p.state(), JobState.class, "state"));

        if (p.salaryMin() != null) {
            b.salaryMin(parseDecimal(p.salaryMin()));
        }
        if (p.includeUnknownSalary() != null) {
            b.includeUnknownSalary(p.includeUnknownSalary());
        }
        if (p.usOnly() != null) {
            b.usOnly(p.usOnly());
        }
        if (p.sponsorship() != null && !p.sponsorship().isBlank()) {
            b.sponsorshipPref(parseEnum(p.sponsorship(), FilterCriteria.SponsorshipPref.class, "sponsorship"));
        }
        if (p.postedWithin() != null && !p.postedWithin().isBlank()) {
            b.postedWithin(parsePostedWithin(p.postedWithin()));
        }
        if (p.dateFrom() != null && !p.dateFrom().isBlank()) {
            b.seenFrom(parseInstant(p.dateFrom(), "dateFrom"));
        }
        if (p.dateTo() != null && !p.dateTo().isBlank()) {
            b.seenTo(parseInstant(p.dateTo(), "dateTo"));
        }
        return b.build();
    }

    // --- helpers ---

    private static List<String> splitList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static <E extends Enum<E>> Set<E> parseEnumSet(String csv, Class<E> type, String field) {
        List<String> tokens = splitList(csv);
        if (tokens.isEmpty()) return Set.of();
        Set<E> out = new LinkedHashSet<>();
        for (String token : tokens) {
            out.add(parseEnum(token, type, field));
        }
        return out;
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String field) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadFilterParam("invalid " + field + " value: '" + raw + "'");
        }
    }

    private static BigDecimal parseDecimal(String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadFilterParam("invalid salaryMin value: '" + raw + "'");
        }
    }

    private static Instant parseInstant(String raw, String field) {
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BadFilterParam("invalid " + field + " (expected ISO-8601 instant): '" + raw + "'");
        }
    }

    /**
     * Friendly relative-window tokens (spec §5 "posted within"): 10m / 30m / 1h / today /
     * week. "today" and "week" are approximated as 24h / 7d rolling windows — honest and
     * simple for v1; a calendar-day variant can come later if measurement warrants.
     */
    private static Duration parsePostedWithin(String raw) {
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "10m" -> Duration.ofMinutes(10);
            case "30m" -> Duration.ofMinutes(30);
            case "1h" -> Duration.ofHours(1);
            case "today" -> Duration.ofHours(24);
            case "week" -> Duration.ofDays(7);
            default -> throw new BadFilterParam(
                    "invalid postedWithin (expected 10m|30m|1h|today|week): '" + raw + "'");
        };
    }
}
