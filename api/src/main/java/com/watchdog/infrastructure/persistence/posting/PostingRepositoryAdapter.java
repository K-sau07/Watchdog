package com.watchdog.infrastructure.persistence.posting;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.PostingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the domain {@link PostingRepository} port over Spring Data JDBC.
 * Maps Posting &lt;-&gt; PostingRow: flattens {@link Salary} into three columns and
 * wraps {@code rawJson} as {@link JsonbString} for the jsonb column. Owns the dedup
 * contract by natural key (spec §8.3).
 */
@Component
class PostingRepositoryAdapter implements PostingRepository {

    private final PostingJdbcRepository jdbc;

    PostingRepositoryAdapter(PostingJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Posting save(Posting posting) {
        boolean isNew = !jdbc.existsById(posting.id().value());
        return toDomain(jdbc.save(toRow(posting, isNew)));
    }

    @Override
    public Optional<Posting> findById(PostingId id) {
        return jdbc.findById(id.value()).map(PostingRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Posting> findByNaturalKey(CompanyId companyId, String atsPostingId) {
        return jdbc.findByCompanyIdAndAtsPostingId(companyId.value(), atsPostingId)
                .map(PostingRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByNaturalKey(CompanyId companyId, String atsPostingId) {
        return jdbc.existsByCompanyIdAndAtsPostingId(companyId.value(), atsPostingId);
    }

    @Override
    public List<Posting> findByCompany(CompanyId companyId) {
        return jdbc.findByCompanyId(companyId.value()).stream()
                .map(PostingRepositoryAdapter::toDomain)
                .toList();
    }

    // --- mapping ---

    private static PostingRow toRow(Posting p, boolean isNew) {
        Salary s = p.salary();
        return new PostingRow(
                p.id().value(), p.companyId().value(), p.atsPostingId(), p.title(),
                p.location(), p.remoteType().name(), p.department(), p.employmentType().name(),
                s.min(), s.max(), s.currency(), p.url(), p.description(),
                p.sponsorshipSignal().name(), p.postedAt(), p.firstSeenAt(),
                new JsonbString(p.rawJson()), isNew);
    }

    private static Posting toDomain(PostingRow r) {
        Salary salary = (r.getSalaryMin() == null && r.getSalaryMax() == null && r.getSalaryCurrency() == null)
                ? Salary.empty()
                : Salary.of(r.getSalaryMin(), r.getSalaryMax(), r.getSalaryCurrency());
        String raw = r.getRaw() == null ? null : r.getRaw().value();
        return new Posting(
                PostingId.of(r.getId()), CompanyId.of(r.getCompanyId()), r.getAtsPostingId(),
                r.getTitle(), r.getLocation(), RemoteType.valueOf(r.getRemoteType()),
                r.getDepartment(), EmploymentType.valueOf(r.getEmploymentType()), salary,
                r.getUrl(), r.getDescription(), SponsorshipSignal.valueOf(r.getSponsorshipSignal()),
                r.getPostedAt(), r.getFirstSeenAt(), raw);
    }
}
