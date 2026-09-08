package com.watchdog.domain.model;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A normalized job posting from an ATS board (spec §4). Immutable.
 *
 * <p>Nullable fields reflect what the ATS actually provides: {@code postedAt} is
 * often absent, as are department, employmentType, and salary details. {@code salary}
 * is never null — use {@link Salary#empty()} for "no data". {@code firstSeenAt} is
 * our own clock (when Watchdog first observed the posting) and is always present.
 *
 * <p>{@code rawJson} preserves the original ATS payload so later heuristics can be
 * re-run without re-fetching (spec §4 "every information").
 */
public record Posting(
        PostingId id,
        CompanyId companyId,
        String atsPostingId,
        String title,
        String location,
        RemoteType remoteType,
        String department,
        EmploymentType employmentType,
        Salary salary,
        String url,
        String description,
        SponsorshipSignal sponsorshipSignal,
        Instant postedAt,
        Instant firstSeenAt,
        String rawJson
) {

    public Posting {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
        requireText(atsPostingId, "atsPostingId");
        requireText(title, "title");
        // Normalize optional-but-non-null domain fields to safe defaults.
        remoteType = remoteType == null ? RemoteType.UNKNOWN : remoteType;
        employmentType = employmentType == null ? EmploymentType.UNKNOWN : employmentType;
        sponsorshipSignal = sponsorshipSignal == null ? SponsorshipSignal.UNKNOWN : sponsorshipSignal;
        salary = salary == null ? Salary.empty() : salary;
    }

    public Optional<Instant> postedAtOpt() {
        return Optional.ofNullable(postedAt);
    }

    public Optional<String> departmentOpt() {
        return Optional.ofNullable(department);
    }

    /**
     * The signature stat — how fresh the posting was when Watchdog caught it
     * (spec §6 "caught N min after posting"). Only meaningful when the ATS gave a
     * real {@code postedAt}; returns empty otherwise so callers never show a faked
     * freshness (spec §8.4). Never negative: if our clock saw it "before" the ATS
     * timestamp (clock skew), the catch time is clamped to zero.
     */
    public Optional<Duration> catchTime() {
        if (postedAt == null) {
            return Optional.empty();
        }
        Duration delta = Duration.between(postedAt, firstSeenAt);
        return Optional.of(delta.isNegative() ? Duration.ZERO : delta);
    }

    /** Convenience: catch time in whole minutes, when known. */
    public Optional<Long> catchMinutes() {
        return catchTime().map(Duration::toMinutes);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
