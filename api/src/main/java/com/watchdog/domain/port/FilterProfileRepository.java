package com.watchdog.domain.port;

import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.FilterProfile;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence for {@link FilterProfile} (spec §4 filter_profile).
 * Implemented in infrastructure (S7). Drives the dashboard's saved-filter CRUD (§7);
 * v1 is single-user (D-WD5) but the contract is user-scoped for later multi-user.
 */
public interface FilterProfileRepository {

    /** Persist a new profile or update an existing one (by id). */
    FilterProfile save(FilterProfile profile);

    Optional<FilterProfile> findById(FilterProfileId id);

    /** All profiles owned by a user (v1: the single user), newest-updated first. */
    List<FilterProfile> findByUser(UserId userId);
}
