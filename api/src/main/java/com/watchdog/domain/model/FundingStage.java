package com.watchdog.domain.model;

/**
 * How far along a company's funding is — the dimension that decides whether one
 * application is worth sending.
 *
 * <p>A posting at a 20-person Series A and the same title at a 5,000-person public
 * company are different products. Watchdog's whole thesis is speed at companies
 * small enough for that speed to matter, so stage is a first-class filter.
 *
 * <p><b>These are not always observed facts.</b> The YC directory, which seeds most
 * of the registry, publishes only "Early" and "Growth" — not series letters. The
 * generator maps those to the values here using team size as a tiebreak, which makes
 * most rows an <em>inference</em>. {@link Company#stageSource()} records where the
 * claim came from and {@link Company#stageAsOf()} when, so a later wire-up of a real
 * funding API can overwrite a guess and be seen to have done so.
 *
 * <p>Ordinal order is meaningful (earlier stage first) so a range filter like
 * "Series A through Series C" can be expressed by comparison.
 */
public enum FundingStage {

    /** Pre-seed and seed. Typically under ~25 people. */
    SEED,

    /** Roughly Series A. First real institutional round. */
    SERIES_A,

    /**
     * Series B and beyond, without a precise letter.
     *
     * <p>The honest shape of what the YC feed can tell us: it says "Growth", which
     * means at least a B but not which one. A funding API would replace this with a
     * specific letter; until then, collapsing B/C/D/E into one bucket is more truthful
     * than inventing a letter we never observed.
     */
    SERIES_B_PLUS,

    /** Late stage — large headcount, past the window this tool is built for. */
    GROWTH,

    /** No stage claim. Hand-curated rows predating stage tracking land here. */
    UNKNOWN;

    /** True when this stage is in the startup window Watchdog targets. */
    public boolean isStartupWindow() {
        return this == SEED || this == SERIES_A || this == SERIES_B_PLUS;
    }

    /** Parses a stored value, tolerating null/unknown text rather than throwing. */
    public static FundingStage parseOrUnknown(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
