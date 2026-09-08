package com.watchdog.infrastructure.persistence.company;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JDBC row for the {@code company} table. Infrastructure-only — the pure
 * domain {@link com.watchdog.domain.model.Company} never carries these annotations.
 *
 * <p>Because IDs are domain-minted (always non-null), Spring Data JDBC can't use
 * "id == null" to tell inserts from updates. We implement {@link Persistable} and
 * carry a transient {@code isNew} flag that the adapter sets explicitly.
 */
@Table("company")
class CompanyRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String name;
    private final String atsSource;
    private final String atsSlug;
    private final boolean active;
    private final Instant lastPolledAt;

    @Transient
    private final boolean isNew;

    CompanyRow(UUID id, String name, String atsSource, String atsSlug,
               boolean active, Instant lastPolledAt, boolean isNew) {
        this.id = id;
        this.name = name;
        this.atsSource = atsSource;
        this.atsSlug = atsSlug;
        this.active = active;
        this.lastPolledAt = lastPolledAt;
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

    String getName() {
        return name;
    }

    String getAtsSource() {
        return atsSource;
    }

    String getAtsSlug() {
        return atsSlug;
    }

    boolean isActive() {
        return active;
    }

    Instant getLastPolledAt() {
        return lastPolledAt;
    }
}
