package com.watchdog.domain.model;

/** What a touch was for. Mirrors the five stages in STAGES.md plus the inbound side. */
public enum EventKind {
    /** Stage 1. A connection request. Carries no ask and no research. */
    INVITE,
    /** Stage 2. The researched question after they accepted and said nothing. */
    OPENER,
    /** A chase on something that went unanswered. */
    FOLLOW_UP,
    /** Stage 5. Only after a real exchange. */
    ASK,
    /** Inbound. They wrote something real. */
    REPLY
}
