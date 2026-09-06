package com.watchdog.domain.model;

/**
 * Work arrangement. As a posting attribute: REMOTE / HYBRID / ONSITE
 * (UNKNOWN when the ATS doesn't say). As a filter preference: ANY means
 * "no preference" (spec §5 Location/Remote).
 */
public enum RemoteType {
    REMOTE,
    HYBRID,
    ONSITE,
    UNKNOWN,
    ANY
}
