package com.watchdog.application.matching;

import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import com.watchdog.domain.port.JobStateRepository;
import com.watchdog.domain.port.PostingRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * The daily-workflow write side (spec §4/§7): set a posting's per-user state
 * (SAVED / APPLIED / HIDDEN) so the radar reflects what the user has handled.
 * Single-user in v1 (D-WD5): every operation is scoped to {@link SingleUser#ID}.
 *
 * <p><b>Upsert by natural key.</b> A posting may not yet have a job-state row. The
 * transition therefore loads the existing record (preserving its id) or mints a fresh
 * {@link JobStateRecord#initial} one, then transitions — never blindly inserts, which
 * would violate the {@code (user_id, posting_id)} unique constraint.
 *
 * <p>Transitioning to APPLIED stamps {@code appliedAt} (handled in the domain record).
 * A null note leaves any existing note untouched; a non-null note replaces it.
 */
@Service
public class JobStateService {

    private final JobStateRepository jobStates;
    private final PostingRepository postings;
    private final Clock clock;

    public JobStateService(JobStateRepository jobStates, PostingRepository postings, Clock clock) {
        this.jobStates = jobStates;
        this.postings = postings;
        this.clock = clock;
    }

    /** Raised when the target posting doesn't exist — the controller renders 404. */
    public static final class PostingNotFound extends RuntimeException {
        public PostingNotFound(PostingId id) {
            super("no posting with id " + id.value());
        }
    }

    /**
     * Set the single user's state for a posting, with an optional note.
     *
     * @param postingId the posting to update (must exist)
     * @param newState  the target workflow state
     * @param note      optional note; null leaves any existing note unchanged
     * @return the persisted job-state record
     * @throws PostingNotFound if no posting has that id
     */
    public JobStateRecord setState(PostingId postingId, JobState newState, String note) {
        if (postings.findById(postingId).isEmpty()) {
            throw new PostingNotFound(postingId);
        }
        Instant now = Instant.now(clock);
        JobStateRecord current = jobStates.findByUserAndPosting(SingleUser.ID, postingId)
                .orElseGet(() -> JobStateRecord.initial(SingleUser.ID, postingId, now));

        JobStateRecord transitioned = current.transitionTo(newState, now);
        if (note != null) {
            transitioned = transitioned.withNote(note, now);
        }
        return jobStates.save(transitioned);
    }
}
