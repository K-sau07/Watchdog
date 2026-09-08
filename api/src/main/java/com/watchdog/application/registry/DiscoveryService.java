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
        for (CompanySeedSource.SeedEntry entry : seedSource.load()) {
            boolean exists = companies.findBySourceAndSlug(entry.source(), entry.slug()).isPresent();
            if (exists) {
                skipped++;
                continue;
            }
            companies.save(Company.register(entry.name(), entry.source(), entry.slug()));
            added++;
        }
        log.info("Registry seed complete: {} added, {} already present", added, skipped);
        return new SeedResult(added, skipped);
    }

    /** Outcome of a seed run. */
    public record SeedResult(int added, int skipped) {
        public int total() {
            return added + skipped;
        }
    }
}
