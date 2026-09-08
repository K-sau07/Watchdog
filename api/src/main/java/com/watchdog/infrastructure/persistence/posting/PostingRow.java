package com.watchdog.infrastructure.persistence.posting;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JDBC row for the {@code posting} table. Infra-only. Salary is stored
 * as three flat columns (matches the domain {@code Salary} value object); {@code raw}
 * is the jsonb payload carried as a JSON String and converted at the driver boundary
 * (see JdbcConfig JsonbConverters). Persistable/isNew handles domain-minted UUIDs.
 */
@Table("posting")
class PostingRow implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID companyId;
    private final String atsPostingId;
    private final String title;
    private final String location;
    private final String remoteType;
    private final String department;
    private final String employmentType;
    private final BigDecimal salaryMin;
    private final BigDecimal salaryMax;
    private final String salaryCurrency;
    private final String url;
    private final String description;
    private final String sponsorshipSignal;
    private final Instant postedAt;
    private final Instant firstSeenAt;
    @Column("raw")
    private final JsonbString raw;

    @Transient
    private final boolean isNew;

    PostingRow(UUID id, UUID companyId, String atsPostingId, String title, String location,
               String remoteType, String department, String employmentType,
               BigDecimal salaryMin, BigDecimal salaryMax, String salaryCurrency,
               String url, String description, String sponsorshipSignal,
               Instant postedAt, Instant firstSeenAt, JsonbString raw, boolean isNew) {
        this.id = id;
        this.companyId = companyId;
        this.atsPostingId = atsPostingId;
        this.title = title;
        this.location = location;
        this.remoteType = remoteType;
        this.department = department;
        this.employmentType = employmentType;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.salaryCurrency = salaryCurrency;
        this.url = url;
        this.description = description;
        this.sponsorshipSignal = sponsorshipSignal;
        this.postedAt = postedAt;
        this.firstSeenAt = firstSeenAt;
        this.raw = raw;
        this.isNew = isNew;
    }

    @Override public UUID getId() { return id; }
    @Override public boolean isNew() { return isNew; }

    UUID getCompanyId() { return companyId; }
    String getAtsPostingId() { return atsPostingId; }
    String getTitle() { return title; }
    String getLocation() { return location; }
    String getRemoteType() { return remoteType; }
    String getDepartment() { return department; }
    String getEmploymentType() { return employmentType; }
    BigDecimal getSalaryMin() { return salaryMin; }
    BigDecimal getSalaryMax() { return salaryMax; }
    String getSalaryCurrency() { return salaryCurrency; }
    String getUrl() { return url; }
    String getDescription() { return description; }
    String getSponsorshipSignal() { return sponsorshipSignal; }
    Instant getPostedAt() { return postedAt; }
    Instant getFirstSeenAt() { return firstSeenAt; }
    JsonbString getRaw() { return raw; }
}
