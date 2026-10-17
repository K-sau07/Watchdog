package com.watchdog.domain.model;

/**
 * Where a person stands, in the vocabulary of the Pipeline vault's STAGES.md.
 *
 * <p>The flow is INVITE → OPENER → LIVE → EXCHANGE → ASK. The first two are a single
 * state here ({@link #REACHED_OUT}) because the app records that an approach was made,
 * not which rung it was on — whether they accepted is LinkedIn's to know.
 *
 * <p>{@link #LIVE} is where a file opens in the vault: they wrote something real. A
 * pleasantry is not a reply.
 */
public enum OutreachStage {
    /** An approach was sent. Nothing has come back that counts as a reply. */
    REACHED_OUT,
    /** They wrote something real. The conversation exists. */
    LIVE,
    /** Two to four real turns, no agenda. */
    EXCHANGE,
    /** The ask has been made. */
    ASKED,
    /** They gave what they could give. */
    WON,
    /** Two unanswered in a row. No more DMs — comments only. */
    NURTURE,
    /** Any hint of irritation. Never contact again. */
    DEAD;

    /** True once they have written something real — i.e. a file exists in the vault. */
    public boolean hasReplied() {
        return this != REACHED_OUT;
    }

    /** True while a conversation is actually running. */
    public boolean isOngoing() {
        return this == LIVE || this == EXCHANGE || this == ASKED;
    }
}
