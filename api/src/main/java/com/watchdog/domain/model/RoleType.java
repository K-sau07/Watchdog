package com.watchdog.domain.model;

/**
 * Whether a role is an individual contributor or a people-manager post.
 *
 * <p>It is the question behind "am I still writing code in this job?", and no ATS
 * publishes it — like {@link Seniority}, it is inferred from the title by
 * {@link RoleTypeParser}.
 */
public enum RoleType {

    /** Individual contributor — the default reading of an engineering title. */
    IC,

    /** A people-manager post: direct reports, not a senior IC. */
    MANAGER,

    /** No title to read. */
    UNKNOWN
}
