package com.watchdog.registry;

import com.watchdog.application.registry.CompanySeedSource;
import com.watchdog.application.registry.CompanySeedSource.SeedEntry;
import com.watchdog.application.registry.DiscoveryService;
import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.port.CompanyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for DiscoveryService using in-memory fakes — no Spring, DB, or file.
 * Focus: idempotent upsert (safe to run repeatedly).
 */
class DiscoveryServiceTest {

    /** Minimal in-memory CompanyRepository fake, keyed by natural key. */
    private static final class FakeCompanyRepo implements CompanyRepository {
        private final Map<String, Company> bySlug = new ConcurrentHashMap<>();

        private static String key(AtsSource s, String slug) { return s + ":" + slug; }

        @Override public Company save(Company c) { bySlug.put(key(c.atsSource(), c.atsSlug()), c); return c; }
        @Override public Optional<Company> findById(CompanyId id) {
            return bySlug.values().stream().filter(c -> c.id().equals(id)).findFirst();
        }
        @Override public Optional<Company> findBySourceAndSlug(AtsSource s, String slug) {
            return Optional.ofNullable(bySlug.get(key(s, slug)));
        }
        @Override public List<Company> findActive() { return List.copyOf(bySlug.values()); }
        @Override public long countBySource(AtsSource s) {
            return bySlug.values().stream().filter(c -> c.atsSource() == s).count();
        }
        int size() { return bySlug.size(); }
    }

    private final CompanySeedSource fakeSeed = () -> List.of(
            new SeedEntry("Stripe", AtsSource.GREENHOUSE, "stripe"),
            new SeedEntry("Linear", AtsSource.ASHBY, "linear"),
            new SeedEntry("Spotify", AtsSource.LEVER, "spotify"));

    @Test
    void seedsAllEntriesFirstRun() {
        FakeCompanyRepo repo = new FakeCompanyRepo();
        var result = new DiscoveryService(fakeSeed, repo).seed();

        assertThat(result.added()).isEqualTo(3);
        assertThat(result.skipped()).isZero();
        assertThat(repo.size()).isEqualTo(3);
    }

    @Test
    void secondRunIsIdempotent() {
        FakeCompanyRepo repo = new FakeCompanyRepo();
        DiscoveryService svc = new DiscoveryService(fakeSeed, repo);
        svc.seed();

        var second = svc.seed();
        assertThat(second.added()).isZero();
        assertThat(second.skipped()).isEqualTo(3);
        assertThat(repo.size()).isEqualTo(3); // no duplicates
    }

    @Test
    void addsOnlyNewEntriesWhenSomeExist() {
        FakeCompanyRepo repo = new FakeCompanyRepo();
        repo.save(Company.register("Linear", AtsSource.ASHBY, "linear")); // pre-existing

        var result = new DiscoveryService(fakeSeed, repo).seed();
        assertThat(result.added()).isEqualTo(2);   // stripe + spotify
        assertThat(result.skipped()).isEqualTo(1); // linear
        assertThat(repo.size()).isEqualTo(3);
    }
}
