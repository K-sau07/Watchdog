package com.watchdog.domain.model;

/**
 * A posting's state in the user's daily workflow.
 * Default feed view hides HIDDEN. See spec §4 / §5.
 */
public enum JobState {
    NEW,
    SAVED,
    APPLIED,
    HIDDEN
}
