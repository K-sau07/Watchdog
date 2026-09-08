package com.watchdog.domain.port;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: the company registry (spec §4 company, §5 D-WD1 seed).
 * Implemented in infrastructure (S2). The agent loop reads active companies to
 * poll (S4); the DiscoveryService seeds/expands it (S5).
 */
public interface CompanyRepository {

    Company save(Company company);

    Optional<Company> findById(CompanyId id);

    /** Lookup by natural key {@code (atsSource, atsSlug)} to avoid duplicate registry rows. */
    Optional<Company> findBySourceAndSlug(AtsSource source, String atsSlug);

    /** Active companies due for polling — the agent loop's work list. */
    List<Company> findActive();

    long countBySource(AtsSource source);
}
