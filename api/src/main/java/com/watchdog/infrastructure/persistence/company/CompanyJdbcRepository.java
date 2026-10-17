package com.watchdog.infrastructure.persistence.company;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for {@link CompanyRow}. Infrastructure-internal;
 * the domain talks to {@link com.watchdog.domain.port.CompanyRepository} via the
 * adapter, not this interface.
 */
interface CompanyJdbcRepository extends CrudRepository<CompanyRow, UUID> {

    Optional<CompanyRow> findByAtsSourceAndAtsSlug(String atsSource, String atsSlug);

    @Query("SELECT * FROM company WHERE active = true")
    List<CompanyRow> findAllActive();

    long countByAtsSource(String atsSource);

    /**
     * Records when we started watching this board, once and only once.
     *
     * <p>The {@code IS NULL} guard is the whole contract: first_polled_at is the fixed
     * point every catch-time is measured against, so a later poll must never move it.
     * Kept out of {@link CompanyRow} deliberately — a mapped column would be rewritten
     * on every save, which is exactly the overwrite this guards against.
     */
    @Modifying
    @Query("UPDATE company SET first_polled_at = :when "
            + "WHERE id = :id AND first_polled_at IS NULL")
    int stampFirstPolledIfAbsent(@Param("id") UUID id, @Param("when") Instant when);
}
