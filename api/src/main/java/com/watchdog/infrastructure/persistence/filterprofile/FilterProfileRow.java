package com.watchdog.infrastructure.persistence.filterprofile;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JDBC row for {@code filter_profile}. Infrastructure-only — the pure
 * domain {@link com.watchdog.domain.model.FilterProfile} never carries these annotations.
 *
 * <p>Postgres {@code TEXT[]} columns map natively to {@code String[]}. Enum sets are
 * stored as their constant names in these arrays; the adapter converts to/from domain
 * enum sets. {@code posted_within_sec} stores a relative window as whole seconds
 * ({@code Duration}); {@code remote_pref}/{@code sponsorship_pref} are single enum names.
 *
 * <p>IDs are domain-minted (never null), so {@link Persistable} + a transient
 * {@code isNew} flag distinguishes insert from update (same pattern as the other rows).
 */
@Table("filter_profile")
class FilterProfileRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID userId;
    private final String name;
    private final String[] roleKeywords;
    private final String[] includeKeywords;
    private final String[] excludeKeywords;
    private final String[] locations;
    private final String remotePref;
    private final String[] seniorities;
    private final String[] employmentTypes;
    private final BigDecimal salaryMin;
    private final String sponsorshipPref;
    private final Long postedWithinSec;
    private final Instant seenFrom;
    private final Instant seenTo;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Transient
    private final boolean isNew;

    FilterProfileRow(UUID id, UUID userId, String name,
                     String[] roleKeywords, String[] includeKeywords, String[] excludeKeywords,
                     String[] locations, String remotePref, String[] seniorities,
                     String[] employmentTypes, BigDecimal salaryMin, String sponsorshipPref,
                     Long postedWithinSec, Instant seenFrom, Instant seenTo,
                     Instant createdAt, Instant updatedAt, boolean isNew) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.roleKeywords = roleKeywords;
        this.includeKeywords = includeKeywords;
        this.excludeKeywords = excludeKeywords;
        this.locations = locations;
        this.remotePref = remotePref;
        this.seniorities = seniorities;
        this.employmentTypes = employmentTypes;
        this.salaryMin = salaryMin;
        this.sponsorshipPref = sponsorshipPref;
        this.postedWithinSec = postedWithinSec;
        this.seenFrom = seenFrom;
        this.seenTo = seenTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isNew = isNew;
    }

    @Override public UUID getId() { return id; }
    @Override public boolean isNew() { return isNew; }

    UUID getUserId() { return userId; }
    String getName() { return name; }
    String[] getRoleKeywords() { return roleKeywords; }
    String[] getIncludeKeywords() { return includeKeywords; }
    String[] getExcludeKeywords() { return excludeKeywords; }
    String[] getLocations() { return locations; }
    String getRemotePref() { return remotePref; }
    String[] getSeniorities() { return seniorities; }
    String[] getEmploymentTypes() { return employmentTypes; }
    BigDecimal getSalaryMin() { return salaryMin; }
    String getSponsorshipPref() { return sponsorshipPref; }
    Long getPostedWithinSec() { return postedWithinSec; }
    Instant getSeenFrom() { return seenFrom; }
    Instant getSeenTo() { return seenTo; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
}
