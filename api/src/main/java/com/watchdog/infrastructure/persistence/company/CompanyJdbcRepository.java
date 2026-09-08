package com.watchdog.infrastructure.persistence.company;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

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
}
