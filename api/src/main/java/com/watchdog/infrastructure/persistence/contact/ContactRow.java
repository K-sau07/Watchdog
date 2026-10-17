package com.watchdog.infrastructure.persistence.contact;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JDBC row for the {@code contact} table. Infrastructure-only — the pure
 * domain {@link com.watchdog.domain.model.Contact} never carries these annotations.
 *
 * <p>No stage, turn or unanswered columns: those are projections over
 * {@code outreach_event} as of V6. Only {@code stage_override} is stored, because a
 * declared stage has no event to derive it from.
 */
@Table("contact")
class ContactRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID userId;
    private final String companyName;
    private final UUID postingId;
    private final String personName;
    private final String personTitle;
    private final String linkedinUrl;
    private final String email;
    private final String personType;
    private final String canGive;
    private final String stageOverride;
    private final Instant reachedOutAt;
    private final Instant lastContactAt;
    private final Instant updatedAt;

    @Transient
    private final boolean isNew;

    ContactRow(UUID id, UUID userId, String companyName, UUID postingId, String personName,
               String personTitle, String linkedinUrl, String email, String personType,
               String canGive, String stageOverride, Instant reachedOutAt,
               Instant lastContactAt, Instant updatedAt, boolean isNew) {
        this.id = id;
        this.userId = userId;
        this.companyName = companyName;
        this.postingId = postingId;
        this.personName = personName;
        this.personTitle = personTitle;
        this.linkedinUrl = linkedinUrl;
        this.email = email;
        this.personType = personType;
        this.canGive = canGive;
        this.stageOverride = stageOverride;
        this.reachedOutAt = reachedOutAt;
        this.lastContactAt = lastContactAt;
        this.updatedAt = updatedAt;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    UUID getUserId() {
        return userId;
    }

    String getCompanyName() {
        return companyName;
    }

    UUID getPostingId() {
        return postingId;
    }

    String getPersonName() {
        return personName;
    }

    String getPersonTitle() {
        return personTitle;
    }

    String getLinkedinUrl() {
        return linkedinUrl;
    }

    String getEmail() {
        return email;
    }

    String getPersonType() {
        return personType;
    }

    String getCanGive() {
        return canGive;
    }

    String getStageOverride() {
        return stageOverride;
    }

    Instant getReachedOutAt() {
        return reachedOutAt;
    }

    Instant getLastContactAt() {
        return lastContactAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
