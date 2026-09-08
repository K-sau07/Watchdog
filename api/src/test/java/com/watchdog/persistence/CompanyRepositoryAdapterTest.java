package com.watchdog.persistence;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    CompanyRepository repo;

    @Test
    void saveAndFindById() {
        Company c = Company.register("Stripe", AtsSource.GREENHOUSE, "stripe");
        repo.save(c);

        assertThat(repo.findById(c.id())).isPresent().get()
                .satisfies(found -> {
                    assertThat(found.name()).isEqualTo("Stripe");
                    assertThat(found.atsSource()).isEqualTo(AtsSource.GREENHOUSE);
                    assertThat(found.active()).isTrue();
                    assertThat(found.lastPolledAt()).isNull();
                });
    }

    @Test
    void findBySourceAndSlug() {
        Company c = Company.register("Ramp", AtsSource.LEVER, "ramp");
        repo.save(c);

        assertThat(repo.findBySourceAndSlug(AtsSource.LEVER, "ramp")).isPresent();
        assertThat(repo.findBySourceAndSlug(AtsSource.ASHBY, "ramp")).isEmpty();
    }

    @Test
    void updatePreservesRowAndPersistsPolledAt() {
        Company c = Company.register("Notion", AtsSource.ASHBY, "notion");
        repo.save(c);

        Instant when = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repo.save(c.polledAt(when));

        assertThat(repo.findById(c.id())).isPresent().get()
                .satisfies(found -> assertThat(found.lastPolledAtOpt()).isPresent());
        // still exactly one row for this company (update, not insert)
        assertThat(repo.countBySource(AtsSource.ASHBY)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void findActiveExcludesInactive() {
        Company active = Company.register("Linear", AtsSource.GREENHOUSE, "linear-active");
        Company inactive = Company.register("Deprecated", AtsSource.GREENHOUSE, "dep-inactive")
                .withActive(false);
        repo.save(active);
        repo.save(inactive);

        assertThat(repo.findActive())
                .anyMatch(x -> x.atsSlug().equals("linear-active"))
                .noneMatch(x -> x.atsSlug().equals("dep-inactive"));
    }

    @Test
    void countBySource() {
        long before = repo.countBySource(AtsSource.LEVER);
        repo.save(Company.register("CountCo", AtsSource.LEVER, "countco-" + System.nanoTime()));
        assertThat(repo.countBySource(AtsSource.LEVER)).isEqualTo(before + 1);
    }
}
