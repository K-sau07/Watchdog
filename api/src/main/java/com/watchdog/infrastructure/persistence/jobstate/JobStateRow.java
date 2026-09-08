package com.watchdog.infrastructure.persistence.jobstate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JDBC row for the {@code job_state} table. Infra-only.
 * Persistable/isNew handles domain-minted UUIDs (see CompanyRow for the rationale).
 */
@Table("job_state")
class JobStateRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID userId;
    private final UUID postingId;
    private final String state;
    private final Instant appliedAt;
    private final String note;
    private final Instant updatedAt;

    @Transient
    private final boolean isNew;

    JobStateRow(UUID id, UUID userId, UUID postingId, String state,
                Instant appliedAt, String note, Instant updatedAt, boolean isNew) {
        this.id = id;
        this.userId = userId;
        this.postingId = postingId;
        this.state = state;
        this.appliedAt = appliedAt;
        this.note = note;
        this.updatedAt = updatedAt;
        this.isNew = isNew;
    }

    @Override public UUID getId() { return id; }
    @Override public boolean isNew() { return isNew; }

    UUID getUserId() { return userId; }
    UUID getPostingId() { return postingId; }
    String getState() { return state; }
    Instant getAppliedAt() { return appliedAt; }
    String getNote() { return note; }
    Instant getUpdatedAt() { return updatedAt; }
}
