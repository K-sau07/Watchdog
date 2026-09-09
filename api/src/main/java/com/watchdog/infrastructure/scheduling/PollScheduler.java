package com.watchdog.infrastructure.scheduling;

import com.watchdog.domain.port.PollingUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires the agent loop on a fixed interval (D-WD2: 2 minutes, configurable via
 * {@code watchdog.polling.interval-ms}). {@link SchedulerLock} ensures only one
 * instance runs a given tick — {@code lockAtMostFor} caps a crashed run's lock,
 * {@code lockAtLeastFor} prevents rapid re-fire if a cycle returns almost instantly.
 *
 * <p>Gated by {@code watchdog.polling.enabled} (default true; false in tests) so it
 * doesn't fire during the test suite.
 */
@Component
@ConditionalOnProperty(name = "watchdog.polling.enabled", havingValue = "true", matchIfMissing = true)
public class PollScheduler {

    private final PollingUseCase pollingUseCase;

    public PollScheduler(PollingUseCase pollingUseCase) {
        this.pollingUseCase = pollingUseCase;
    }

    @Scheduled(fixedDelayString = "${watchdog.polling.interval-ms:3600000}")
    @SchedulerLock(name = "watchdog-poll-cycle", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
    public void poll() {
        pollingUseCase.runOnce();
    }
}
