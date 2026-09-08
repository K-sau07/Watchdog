package com.watchdog.domain.model;

import com.watchdog.domain.id.CompanyId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A company whose ATS board Watchdog polls (spec §4).
 *
 * <p>{@code atsSlug} is the {company} segment in the ATS API URL. {@code lastPolledAt}
 * is null until the first poll. Immutable — state transitions (e.g. marking polled)
 * return a new instance.
 */
public record Company(
        CompanyId id,
        String name,
        AtsSource atsSource,
        String atsSlug,
        boolean active,
        Instant lastPolledAt
) {

    public Company {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(atsSource, "atsSource must not be null");
        requireText(name, "name");
        requireText(atsSlug, "atsSlug");
    }

    /** Factory for a newly-registered, active company that has never been polled. */
    public static Company register(String name, AtsSource atsSource, String atsSlug) {
        return new Company(CompanyId.generate(), name, atsSource, atsSlug, true, null);
    }

    public Optional<Instant> lastPolledAtOpt() {
        return Optional.ofNullable(lastPolledAt);
    }

    /** Returns a copy marked as polled at the given instant. */
    public Company polledAt(Instant when) {
        Objects.requireNonNull(when, "when must not be null");
        return new Company(id, name, atsSource, atsSlug, active, when);
    }

    /** Returns a copy with the given active flag. */
    public Company withActive(boolean newActive) {
        return new Company(id, name, atsSource, atsSlug, newActive, lastPolledAt);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
