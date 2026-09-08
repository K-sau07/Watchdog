package com.watchdog.persistence;

import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.JobStateRepository;
import com.watchdog.domain.port.PostingRepository;
import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobStateRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    JobStateRepository jobStates;
    @Autowired
    CompanyRepository companies;
    @Autowired
    PostingRepository postings;
    @Autowired
    JdbcClient jdbc;

    /** Insert a fixture app_user directly (its adapter is deferred to S7). */
    private UserId newUser() {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO app_user (id, username) VALUES (?, ?)")
                .params(id, "user-" + id).update();
        return UserId.of(id);
    }

    private PostingId newPosting() {
        Company c = Company.register("Co-" + System.nanoTime(), AtsSource.GREENHOUSE,
                "slug-" + System.nanoTime());
        companies.save(c);
        Posting p = new Posting(
                PostingId.generate(), c.id(), "ats-" + System.nanoTime(), "SWE",
                "NYC", RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "desc", SponsorshipSignal.UNKNOWN, null,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), null);
        postings.save(p);
        return p.id();
    }

    @Test
    void saveInitialAndFind() {
        UserId user = newUser();
        PostingId posting = newPosting();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        JobStateRecord initial = JobStateRecord.initial(user, posting, now);
        jobStates.save(initial);

        assertThat(jobStates.findByUserAndPosting(user, posting)).isPresent().get()
                .satisfies(found -> {
                    assertThat(found.state()).isEqualTo(JobState.NEW);
                    assertThat(found.appliedAt()).isNull();
                });
    }

    @Test
    void transitionToAppliedPersistsAppliedAt() {
        UserId user = newUser();
        PostingId posting = newPosting();
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-01-01T11:00:00Z");

        JobStateRecord initial = JobStateRecord.initial(user, posting, t0);
        jobStates.save(initial);
        jobStates.save(initial.transitionTo(JobState.APPLIED, t1));

        assertThat(jobStates.findByUserAndPosting(user, posting)).isPresent().get()
                .satisfies(found -> {
                    assertThat(found.state()).isEqualTo(JobState.APPLIED);
                    assertThat(found.appliedAtOpt()).isPresent();
                });
    }

    @Test
    void findMissingReturnsEmpty() {
        assertThat(jobStates.findByUserAndPosting(UserId.generate(), PostingId.generate()))
                .isEmpty();
    }
}
