package com.watchdog.domain.model;

/**
 * How a touch reached someone.
 *
 * <p>{@link #LINKEDIN_INVITE} is deliberately separate from {@link #LINKEDIN_DM}. STAGES.md:
 * <em>"An invite note is not a message. It is an invite. The counter starts after the
 * accept."</em> Folding the two together would charge an invite against the unanswered
 * cap and halve the budget before anything was actually said.
 */
public enum Channel {
    LINKEDIN_INVITE,
    LINKEDIN_DM,
    EMAIL;

    /**
     * Whether a send on this channel counts against the unanswered cap. Only an invite
     * does not.
     */
    public boolean countsAsMessage() {
        return this != LINKEDIN_INVITE;
    }

    /**
     * The channel a follow-up would go out on.
     *
     * <p>An unanswered invite is chased with a DM rather than a second invite — LinkedIn
     * does not allow a second invite, and the person may simply not have accepted yet.
     */
    public Channel followUpChannel() {
        return this == LINKEDIN_INVITE ? LINKEDIN_DM : this;
    }
}
