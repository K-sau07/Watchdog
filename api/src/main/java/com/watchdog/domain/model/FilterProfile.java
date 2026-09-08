package com.watchdog.domain.model;

import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.id.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * A named, persisted, user-owned filter (spec §4 filter_profile). Wraps a
 * {@link FilterCriteria} (the match input, reused rather than re-declared) with
 * persistence identity: id, owning user, display name, and timestamps.
 *
 * <p>Immutable; edits return a new instance. The dashboard loads the user's profile as
 * the default feed filter and lets every dimension be adjusted live (spec §5/§7).
 */
public record FilterProfile(
        FilterProfileId id,
        UserId userId,
        String name,
        FilterCriteria criteria,
        Instant createdAt,
        Instant updatedAt
) {

    public FilterProfile {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(criteria, "criteria must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        requireText(name, "name");
    }

    /** A freshly-created profile owned by {@code userId}. */
    public static FilterProfile create(UserId userId, String name, FilterCriteria criteria, Instant now) {
        return new FilterProfile(FilterProfileId.generate(), userId, name, criteria, now, now);
    }

    /** Returns a copy with new criteria/name, stamped as updated at {@code now}. */
    public FilterProfile update(String newName, FilterCriteria newCriteria, Instant now) {
        return new FilterProfile(id, userId, newName, newCriteria, createdAt, now);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
