package com.watchdog.domain.model;

/**
 * Visa/sponsorship signal parsed heuristically from the posting body (spec §5).
 * UNKNOWN when the description gives no clear signal.
 */
public enum SponsorshipSignal {
    OFFERED,
    NOT_OFFERED,
    UNKNOWN
}
