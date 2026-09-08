package com.watchdog.application.matching;

import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.port.FilterProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Saved-filter CRUD for the dashboard (spec §7), single-user in v1 (D-WD5).
 *
 * <p><b>Auto-provision on first read.</b> The sole user starts with no profile; rather
 * than a 404 dance, {@link #getOrCreateDefault()} lazily creates a sensible default the
 * first time it's read (idempotent thereafter). The default encodes the owner's actual
 * hunt: early-career SWE targeting NEW_GRAD/JUNIOR/MID, with no experience-floor
 * exclusion (owner clears a ~2-year floor) — a filter-profile default, deliberately not
 * baked into the user-agnostic SeniorityParser.
 */
@Service
public class FilterProfileService {

    private final FilterProfileRepository profiles;
    private final Clock clock;

    public FilterProfileService(FilterProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    /** Raised when an explicitly-requested profile id doesn't exist — controller → 404. */
    public static final class ProfileNotFound extends RuntimeException {
        public ProfileNotFound(FilterProfileId id) {
            super("no filter profile with id " + id.value());
        }
    }

    /**
     * The single user's default profile, creating it on first access. Returns the
     * most-recently-updated profile if any exist; otherwise persists and returns a
     * fresh default.
     */
    public FilterProfile getOrCreateDefault() {
        List<FilterProfile> existing = profiles.findByUser(SingleUser.ID);
        if (!existing.isEmpty()) {
            return existing.get(0); // findByUser is ORDER BY updated_at DESC
        }
        Instant now = Instant.now(clock);
        return profiles.save(FilterProfile.create(
                SingleUser.ID, "Default", defaultCriteria(), now));
    }

    /** Fetch a specific profile by id, or throw {@link ProfileNotFound}. */
    public FilterProfile getById(FilterProfileId id) {
        return profiles.findById(id).orElseThrow(() -> new ProfileNotFound(id));
    }

    /** Update a profile's name + criteria; throws {@link ProfileNotFound} if unknown. */
    public FilterProfile update(FilterProfileId id, String name, FilterCriteria criteria) {
        FilterProfile current = getById(id);
        Instant now = Instant.now(clock);
        return profiles.save(current.update(name, criteria, now));
    }

    /**
     * The owner's default hunt (S6.4/S7 profile default, not the parser): early-career
     * SWE, targeting NEW_GRAD/JUNIOR/MID, no experience-floor exclusion.
     */
    static FilterCriteria defaultCriteria() {
        return FilterCriteria.builder()
                .roleKeywords(List.of("software engineer", "swe"))
                .seniorities(Set.of(Seniority.NEW_GRAD, Seniority.JUNIOR, Seniority.MID))
                .build();
    }
}
