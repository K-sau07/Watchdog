package com.watchdog.application.polling;

import com.watchdog.domain.port.PollingUseCase;
import com.watchdog.domain.port.PollingUseCase.PollCycleResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OnDemandPollServiceTest {

    private final AtomicInteger runs = new AtomicInteger();
    private final PollingUseCase polling = new PollingUseCase() {
        @Override public PollCycleResult runOnce() {
            return new PollCycleResult(Instant.now(), 10, 0, runs.incrementAndGet(), Optional.empty());
        }
        @Override public PollCycleResult runStaggered() { return runOnce(); }
    };

    // A mutable clock we can advance.
    private Instant now = Instant.parse("2026-09-08T12:00:00Z");
    private final java.time.Clock clock = new java.time.Clock() {
        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return now; }
    };

    @Test
    void firstCallRunsImmediately() {
        var svc = new OnDemandPollService(polling, clock, 120_000);
        var result = svc.refreshNow();
        assertThat(result).isInstanceOf(OnDemandPollService.Result.Ran.class);
        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    void secondCallWithinCooldownIsRejected() {
        var svc = new OnDemandPollService(polling, clock, 120_000);
        svc.refreshNow();                 // runs
        now = now.plus(Duration.ofSeconds(30)); // 30s later, still cooling
        var result = svc.refreshNow();

        assertThat(result).isInstanceOf(OnDemandPollService.Result.CoolingDown.class);
        assertThat(((OnDemandPollService.Result.CoolingDown) result).retryAfterSeconds())
                .isEqualTo(90); // 120 - 30
        assertThat(runs.get()).isEqualTo(1); // did NOT run again
    }

    @Test
    void callAfterCooldownRunsAgain() {
        var svc = new OnDemandPollService(polling, clock, 120_000);
        svc.refreshNow();
        now = now.plus(Duration.ofSeconds(120));
        var result = svc.refreshNow();
        assertThat(result).isInstanceOf(OnDemandPollService.Result.Ran.class);
        assertThat(runs.get()).isEqualTo(2);
    }
}
