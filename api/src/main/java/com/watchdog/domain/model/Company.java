package com.watchdog.domain.model;

import com.watchdog.domain.id.CompanyId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A company whose ATS board Watchdog polls (spec §4).
 *
 * <p>{@code atsSlug} is the {company} segment in the ATS API URL. {@code lastPolledAt}
 * is null until the first poll. Immutable — state transitions (e.g. marking polled)
 * return a new instance.
 *
 * <p>The three stage fields travel together: the claim, where it came from, and when.
 * A value without provenance ages into a lie, so {@link #stageSource()} and
 * {@link #stageAsOf()} are not optional metadata — they are what makes the claim
 * refutable. See {@link FundingStage} for why most values are inferences.
 */
public record Company(
        CompanyId id,
        String name,
        AtsSource atsSource,
        String atsSlug,
        boolean active,
        Instant lastPolledAt,
        FundingStage fundingStage,
        StageSource stageSource,
        LocalDate stageAsOf
) {

    public Company {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(atsSource, "atsSource must not be null");
        requireText(name, "name");
        requireText(atsSlug, "atsSlug");
        // Never null: an absent claim is UNKNOWN, which callers can filter on.
        fundingStage = fundingStage == null ? FundingStage.UNKNOWN : fundingStage;
        stageSource = stageSource == null ? StageSource.UNKNOWN : stageSource;
    }

    /** Factory for a newly-registered, active company that has never been polled. */
    public static Company register(String name, AtsSource atsSource, String atsSlug) {
        return register(name, atsSource, atsSlug, FundingStage.UNKNOWN, StageSource.UNKNOWN, null);
    }

    /** Factory carrying a stage claim and its provenance. */
    public static Company register(String name, AtsSource atsSource, String atsSlug,
                                   FundingStage fundingStage, StageSource stageSource,
                                   LocalDate stageAsOf) {
        return new Company(CompanyId.generate(), name, atsSource, atsSlug, true, null,
                fundingStage, stageSource, stageAsOf);
    }

    public Optional<Instant> lastPolledAtOpt() {
        return Optional.ofNullable(lastPolledAt);
    }

    public Optional<LocalDate> stageAsOfOpt() {
        return Optional.ofNullable(stageAsOf);
    }

    /** Returns a copy marked as polled at the given instant. */
    public Company polledAt(Instant when) {
        Objects.requireNonNull(when, "when must not be null");
        return new Company(id, name, atsSource, atsSlug, active, when,
                fundingStage, stageSource, stageAsOf);
    }

    /** Returns a copy with the given active flag. */
    public Company withActive(boolean newActive) {
        return new Company(id, name, atsSource, atsSlug, newActive, lastPolledAt,
                fundingStage, stageSource, stageAsOf);
    }

    /**
     * Returns a copy carrying a new stage claim, but only when the incoming source
     * outranks the one already stored. A nightly YC refresh must not quietly
     * downgrade a value a funding API or a human established.
     *
     * <p>Returns {@code this} whenever nothing would change, so callers can use
     * identity to tell a real update from a re-seed that restated what was already
     * stored. A refreshed {@code asOf} does count as a change: re-confirming a claim
     * today is exactly what keeps the date from ageing into a lie.
     */
    public Company withStage(FundingStage newStage, StageSource newSource, LocalDate asOf) {
        Objects.requireNonNull(newSource, "newSource must not be null");
        if (stageSource != newSource && !stageSource.supersededBy(newSource)) {
            return this;
        }
        FundingStage resolved = newStage == null ? FundingStage.UNKNOWN : newStage;
        if (resolved == fundingStage && newSource == stageSource
                && Objects.equals(asOf, stageAsOf)) {
            return this;
        }
        return new Company(id, name, atsSource, atsSlug, active, lastPolledAt,
                resolved, newSource, asOf);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
