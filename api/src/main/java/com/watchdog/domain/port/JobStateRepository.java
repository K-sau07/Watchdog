package com.watchdog.domain.port;

import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.JobStateRecord;

import java.util.Optional;

/**
 * Outbound port: per-user workflow state for postings (spec §4 job_state).
 * Implemented in infrastructure (S2); drives the save/applied/hide workflow (S7).
 * Natural key is {@code (userId, postingId)}.
 */
public interface JobStateRepository {

    JobStateRecord save(JobStateRecord record);

    Optional<JobStateRecord> findByUserAndPosting(UserId userId, PostingId postingId);
}
