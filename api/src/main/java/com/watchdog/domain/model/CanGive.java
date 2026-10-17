package com.watchdog.domain.model;

/**
 * What this person is in a position to give. Decides the shape of the stage-5 ask:
 * a referral, fifteen minutes, or a pointer to whoever owns the req.
 *
 * <p>Absent (null) is meaningful and is not a value here — STAGES.md refuses the ask
 * outright when nobody can give anything, because a forced bridge is worse than silence.
 */
public enum CanGive {
    /** Can refer. The ask acknowledges they are staking their name. */
    REFER,
    /** Can interview. Fifteen minutes, never a referral. */
    INTERVIEW,
    /** Can point you at whoever owns it. */
    ROUTE
}
