package com.watchdog.domain.model;

import java.util.Locale;

/**
 * Where a {@link FundingStage} claim came from.
 *
 * <p>Stage decays — a company that was Series A last year may be Series C now — and
 * the sources differ in how much they can be trusted. Recording provenance alongside
 * the value means a refresh can tell "nobody has ever checked this" apart from "a
 * funding API said so last Tuesday", and overwrite only what it should.
 */
public enum StageSource {

    /**
     * Derived from the YC directory's coarse Early/Growth flag plus team size.
     * An inference, not a funding fact — see {@link FundingStage}.
     */
    YC_DIRECTORY,

    /** Hand-entered. The pre-existing curated rows, which carry no stage claim. */
    MANUAL,

    /**
     * A licensed funding API (Crunchbase, Harmonic, Parallel). Not wired up yet;
     * the constant exists so the schema does not need a migration when it is.
     */
    FUNDING_API,

    /** No provenance recorded. */
    UNKNOWN;

    public static StageSource parseOrUnknown(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /** True when a claim from {@code other} should replace one from this source. */
    public boolean supersededBy(StageSource other) {
        return rank(other) > rank(this);
    }

    private static int rank(StageSource s) {
        return switch (s) {
            case FUNDING_API -> 3;   // observed, licensed
            case MANUAL -> 2;        // a human looked
            case YC_DIRECTORY -> 1;  // inferred
            case UNKNOWN -> 0;
        };
    }
}
