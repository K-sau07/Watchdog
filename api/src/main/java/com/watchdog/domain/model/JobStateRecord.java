package com.watchdog.domain.model;

import com.watchdog.domain.id.JobStateId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A user's workflow state for a posting (spec §4 job_state). Immutable; transitions
 * return new instances. {@code appliedAt} is set only when state becomes APPLIED;
 * {@code note} is optional. Named JobStateRecord to avoid clashing with the
 * {@link JobState} enum.
 */
public record JobStateRecord(
        JobStateId id,
        UserId userId,
        PostingId postingId,
        JobState state,
        Instant appliedAt,
        String note,
        Instant updatedAt
) {

    public JobStateRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(postingId, "postingId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** Initial NEW state for a freshly-surfaced posting. */
    public static JobStateRecord initial(UserId userId, PostingId postingId, Instant now) {
        return new JobStateRecord(
                JobStateId.generate(), userId, postingId, JobState.NEW, null, null, now);
    }

    public Optional<Instant> appliedAtOpt() {
        return Optional.ofNullable(appliedAt);
    }

    public Optional<String> noteOpt() {
        return Optional.ofNullable(note);
    }

    /**
     * Transition to a new state at {@code now}. Moving to APPLIED stamps appliedAt
     * (preserving an earlier one if already applied); other states leave it as-is.
     */
    public JobStateRecord transitionTo(JobState newState, Instant now) {
        Objects.requireNonNull(newState, "newState must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Instant newAppliedAt = newState == JobState.APPLIED
                ? (appliedAt != null ? appliedAt : now)
                : appliedAt;
        return new JobStateRecord(id, userId, postingId, newState, newAppliedAt, note, now);
    }

    /** Returns a copy with the given note. */
    public JobStateRecord withNote(String newNote, Instant now) {
        return new JobStateRecord(id, userId, postingId, state, appliedAt, newNote, now);
    }
}
