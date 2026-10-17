package com.watchdog.domain.model;

/**
 * Who this person is, in ROUTER.md's terms. Sets the register and the research depth;
 * it does <em>not</em> set the ask — {@link CanGive} does.
 *
 * <p>Same vocabulary as the {@code type} field in Pipeline/people/_TEMPLATE.md so the
 * app and the vault do not drift into two classification schemes.
 */
public enum PersonType {
    /** Best referral odds. Opener is about their work. Register: peer. */
    ENGINEER,
    /** Opener is about their reqs. Brevity is the whole game. Routes rather than refers. */
    RECRUITER,
    /** Owns the req. Interviews, never refers — asking tells them you don't know their job. */
    HIRING_MANAGER,
    /** A warmth modifier rather than an ask. Only claimed when LinkedIn confirms it. */
    ALUM,
    /** Senior at the right company and the wrong org. Ask for the process, not the job. */
    SENIOR_OFF_REQ
}
