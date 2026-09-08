package com.watchdog.infrastructure.persistence.jobstate;

import com.watchdog.domain.id.JobStateId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import com.watchdog.domain.port.JobStateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter implementing the domain {@link JobStateRepository} port over Spring Data JDBC.
 * Maps JobStateRecord &lt;-&gt; JobStateRow. Natural key is (user_id, posting_id),
 * enforced by a unique index (spec §4).
 */
@Component
class JobStateRepositoryAdapter implements JobStateRepository {

    private final JobStateJdbcRepository jdbc;

    JobStateRepositoryAdapter(JobStateJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public JobStateRecord save(JobStateRecord record) {
        boolean isNew = !jdbc.existsById(record.id().value());
        return toDomain(jdbc.save(toRow(record, isNew)));
    }

    @Override
    public Optional<JobStateRecord> findByUserAndPosting(UserId userId, PostingId postingId) {
        return jdbc.findByUserIdAndPostingId(userId.value(), postingId.value())
                .map(JobStateRepositoryAdapter::toDomain);
    }

    // --- mapping ---

    private static JobStateRow toRow(JobStateRecord r, boolean isNew) {
        return new JobStateRow(
                r.id().value(), r.userId().value(), r.postingId().value(),
                r.state().name(), r.appliedAt(), r.note(), r.updatedAt(), isNew);
    }

    private static JobStateRecord toDomain(JobStateRow r) {
        return new JobStateRecord(
                JobStateId.of(r.getId()), UserId.of(r.getUserId()), PostingId.of(r.getPostingId()),
                JobState.valueOf(r.getState()), r.getAppliedAt(), r.getNote(), r.getUpdatedAt());
    }
}
