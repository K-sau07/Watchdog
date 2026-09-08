package com.watchdog.domain.model;

/**
 * Seniority inferred from title + keywords (spec §5, heuristic in v1).
 * The default hunt targets INTERN / NEW_GRAD / JUNIOR.
 * UNKNOWN when the title gives no clear signal.
 */
public enum Seniority {
    INTERN,
    NEW_GRAD,
    JUNIOR,
    MID,
    SENIOR,
    UNKNOWN
}
