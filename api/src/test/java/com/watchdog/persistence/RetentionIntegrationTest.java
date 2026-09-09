package com.watchdog.persistence;

import com.watchdog.application.polling.RetentionService;
import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.PostingRepository;
import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionIntegrationTest extends PostgresIntegrationTest {

    @Autowired PostingRepository postings;
    @Autowired CompanyRepository companies;
    @Autowired JdbcClient jdbc;

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    private CompanyId company() {
        Company c = Company.register("Co-" + System.nanoTime(), AtsSource.GREENHOUSE, "s" + System.nanoTime());
        companies.save(c);
        return c.id();
    }

    private Posting posting(CompanyId co, String atsId, Instant postedAt) {
        return new Posting(PostingId.generate(), co, atsId, "SWE", "NYC", RemoteType.REMOTE,
                "Eng", EmploymentType.FULL_TIME, Salary.empty(), "https://x/y", "d",
                SponsorshipSignal.UNKNOWN, postedAt,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), null);
    }

    @Test
    void deletesStaleUnsavedButKeepsRecentNullDateAndSaved() {
        CompanyId co = company();
        Instant old = NOW.minus(30, ChronoUnit.DAYS);   // stale
        Instant recent = NOW.minus(5, ChronoUnit.DAYS);  // fresh

        Posting stale = postings.save(posting(co, "stale-" + System.nanoTime(), old));
        Posting recentP = postings.save(posting(co, "recent-" + System.nanoTime(), recent));
        Posting nullDate = postings.save(posting(co, "nulldate-" + System.nanoTime(), null));
        Posting staleButSaved = postings.save(posting(co, "saved-" + System.nanoTime(), old));

        // Mark staleButSaved with a job_state row (protects it). Needs a user (FK).
        UUID userId = UUID.randomUUID();
        jdbc.sql("INSERT INTO app_user (id, username) VALUES (?, ?)")
                .params(userId, "u-" + userId).update();
        jdbc.sql("INSERT INTO job_state (id, user_id, posting_id, state, updated_at) "
                        + "VALUES (?, ?, ?, 'SAVED', now())")
                .params(UUID.randomUUID(), userId, staleButSaved.id().value()).update();

        int deleted = postings.deleteStalePostedBefore(NOW.minus(21, ChronoUnit.DAYS));

        // Only the stale, unsaved posting is gone.
        assertThat(postings.findById(stale.id())).isEmpty();
        assertThat(postings.findById(recentP.id())).isPresent();      // recent → kept
        assertThat(postings.findById(nullDate.id())).isPresent();     // unknown date → kept
        assertThat(postings.findById(staleButSaved.id())).isPresent(); // saved → protected
        assertThat(deleted).isGreaterThanOrEqualTo(1);
    }
}
