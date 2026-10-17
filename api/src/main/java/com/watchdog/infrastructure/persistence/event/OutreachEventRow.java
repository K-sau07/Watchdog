package com.watchdog.infrastructure.persistence.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Spring Data JDBC row for {@code outreach_event}. Infrastructure-only. */
@Table("outreach_event")
class OutreachEventRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID contactId;
    private final String direction;
    private final String channel;
    private final String kind;
    private final String status;
    private final String body;
    private final Instant occurredAt;

    @Transient
    private final boolean isNew;

    OutreachEventRow(UUID id, UUID contactId, String direction, String channel, String kind,
                     String status, String body, Instant occurredAt, boolean isNew) {
        this.id = id;
        this.contactId = contactId;
        this.direction = direction;
        this.channel = channel;
        this.kind = kind;
        this.status = status;
        this.body = body;
        this.occurredAt = occurredAt;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return id;
    }

    /** Always true: the log is append-only, so a row is never updated in place. */
    @Override
    public boolean isNew() {
        return isNew;
    }

    UUID getContactId() {
        return contactId;
    }

    String getDirection() {
        return direction;
    }

    String getChannel() {
        return channel;
    }

    String getKind() {
        return kind;
    }

    String getStatus() {
        return status;
    }

    String getBody() {
        return body;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }
}
