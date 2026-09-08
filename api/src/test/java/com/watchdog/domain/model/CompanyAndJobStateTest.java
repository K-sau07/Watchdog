package com.watchdog.domain.model;

import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyAndJobStateTest {

    @Test
    void registerCreatesActiveNeverPolledCompany() {
        Company c = Company.register("Stripe", AtsSource.GREENHOUSE, "stripe");
        assertThat(c.id()).isNotNull();
        assertThat(c.active()).isTrue();
        assertThat(c.lastPolledAt()).isNull();
        assertThat(c.lastPolledAtOpt()).isEmpty();
    }

    @Test
    void polledAtReturnsCopyWithTimestamp() {
        Company c = Company.register("Stripe", AtsSource.GREENHOUSE, "stripe");
        Instant when = Instant.parse("2026-01-01T10:00:00Z");
        Company polled = c.polledAt(when);
        assertThat(polled.lastPolledAtOpt()).contains(when);
        assertThat(c.lastPolledAt()).isNull(); // original unchanged (immutable)
    }

    @Test
    void blankSlugRejected() {
        assertThatThrownBy(() -> Company.register("Stripe", AtsSource.LEVER, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initialJobStateIsNew() {
        Instant now = Instant.now();
        JobStateRecord s = JobStateRecord.initial(UserId.generate(), PostingId.generate(), now);
        assertThat(s.state()).isEqualTo(JobState.NEW);
        assertThat(s.appliedAt()).isNull();
    }

    @Test
    void transitionToAppliedStampsAppliedAt() {
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-01-01T11:00:00Z");
        JobStateRecord s = JobStateRecord.initial(UserId.generate(), PostingId.generate(), t0);
        JobStateRecord applied = s.transitionTo(JobState.APPLIED, t1);
        assertThat(applied.state()).isEqualTo(JobState.APPLIED);
        assertThat(applied.appliedAtOpt()).contains(t1);
        assertThat(applied.updatedAt()).isEqualTo(t1);
    }

    @Test
    void reApplyingPreservesOriginalAppliedAt() {
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-01-01T11:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T12:00:00Z");
        JobStateRecord applied = JobStateRecord
                .initial(UserId.generate(), PostingId.generate(), t0)
                .transitionTo(JobState.APPLIED, t1)
                .transitionTo(JobState.APPLIED, t2);
        assertThat(applied.appliedAtOpt()).contains(t1); // not overwritten by t2
    }

    @Test
    void transitionToNonAppliedLeavesAppliedAtUnchanged() {
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-01-01T11:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T12:00:00Z");
        JobStateRecord hidden = JobStateRecord
                .initial(UserId.generate(), PostingId.generate(), t0)
                .transitionTo(JobState.APPLIED, t1)
                .transitionTo(JobState.HIDDEN, t2);
        assertThat(hidden.state()).isEqualTo(JobState.HIDDEN);
        assertThat(hidden.appliedAtOpt()).contains(t1); // kept
    }
}
