package com.watchdog.infrastructure.persistence.company;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.port.CompanyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the domain {@link CompanyRepository} port over Spring Data
 * JDBC. Maps between the pure domain {@link Company} and the annotated
 * {@link CompanyRow}, keeping persistence concerns out of the domain.
 */
@Component
class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyJdbcRepository jdbc;

    CompanyRepositoryAdapter(CompanyJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Company save(Company company) {
        boolean isNew = !jdbc.existsById(company.id().value());
        CompanyRow saved = jdbc.save(toRow(company, isNew));
        return toDomain(saved);
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return jdbc.findById(id.value()).map(CompanyRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Company> findBySourceAndSlug(AtsSource source, String atsSlug) {
        return jdbc.findByAtsSourceAndAtsSlug(source.name(), atsSlug)
                .map(CompanyRepositoryAdapter::toDomain);
    }

    @Override
    public List<Company> findActive() {
        return jdbc.findAllActive().stream()
                .map(CompanyRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public long countBySource(AtsSource source) {
        return jdbc.countByAtsSource(source.name());
    }

    // --- mapping ---

    private static CompanyRow toRow(Company c, boolean isNew) {
        return new CompanyRow(
                c.id().value(), c.name(), c.atsSource().name(), c.atsSlug(),
                c.active(), c.lastPolledAt(), isNew);
    }

    private static Company toDomain(CompanyRow r) {
        return new Company(
                CompanyId.of(r.getId()), r.getName(), AtsSource.valueOf(r.getAtsSource()),
                r.getAtsSlug(), r.isActive(), r.getLastPolledAt());
    }
}
