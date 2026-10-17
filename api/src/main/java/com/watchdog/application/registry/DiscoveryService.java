package com.watchdog.application.registry;

import com.watchdog.domain.model.Company;
import com.watchdog.domain.port.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Seeds and grows the company registry (spec §5 / D-WD1). Reads the verified seed set
 * and upserts it idempotently: a company already present (by natural key
 * source+slug) is left untouched, so this is safe to run on every startup.
 *
 * <p>Registry auto-expansion (discovering new boards automatically) is a documented
 * fast-follow (D-WD1); v1 grows by appending verified rows to the seed file.
 */
@Service
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final CompanySeedSource seedSource;
    private final CompanyRepository companies;

    public DiscoveryService(CompanySeedSource seedSource, CompanyRepository companies) {
        this.seedSource = seedSource;
        this.companies = companies;
    }

    /**
     * Register any seed companies not already in the registry.
     *
     * @return a summary of how many were added vs. already present
     */
    public SeedResult seed() {
        int added = 0;
        int skipped = 0;
        int restaged = 0;
        for (CompanySeedSource.SeedEntry entry : seedSource.load()) {
            var existing = companies.findBySourceAndSlug(entry.source(), entry.slug());
            if (existing.isPresent()) {
                // Already registered, but the seed may now carry a stage claim the
                // stored row predates — the curated rows were all added before stage
                // tracking existed. withStage refuses a downgrade, so re-seeding is
                // safe to run repeatedly and cannot clobber a better-sourced value.
                Company current = existing.get();
                Company updated = current.withStage(
                        entry.stage(), entry.stageSource(), entry.stageAsOf());
                if (updated != current) {
                    companies.save(updated);
                    restaged++;
                } else {
                    skipped++;
                }
                continue;
            }
            companies.save(Company.register(
                    entry.name(), entry.source(), entry.slug(),
                    entry.stage(), entry.stageSource(), entry.stageAsOf()));
            added++;
        }
        log.info("Registry seed complete: {} added, {} restaged, {} unchanged",
                added, restaged, skipped);
        return new SeedResult(added, skipped, restaged);
    }

    /** Outcome of a seed run. */
    public record SeedResult(int added, int skipped, int restaged) {

        /** Back-compat for callers that predate stage tracking. */
        public SeedResult(int added, int skipped) {
            this(added, skipped, 0);
        }

        public int total() {
            return added + skipped + restaged;
        }
    }
}
