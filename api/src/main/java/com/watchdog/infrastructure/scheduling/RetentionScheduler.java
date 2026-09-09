package com.watchdog.infrastructure.scheduling;

import com.watchdog.application.polling.RetentionService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically prunes stale postings (D-WD18) via {@link RetentionService}. Runs on a
 * fixed interval (default daily, {@code watchdog.retention.interval-ms}), ShedLock-guarded
 * so only one instance prunes per tick. Gated by {@code watchdog.polling.enabled} (off in
 * tests) so it doesn't fire during the suite.
 */
@Component
@ConditionalOnProperty(name = "watchdog.polling.enabled", havingValue = "true", matchIfMissing = true)
public class RetentionScheduler {

    private final RetentionService retentionService;

    public RetentionScheduler(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(fixedDelayString = "${watchdog.retention.interval-ms:86400000}")
    @SchedulerLock(name = "watchdog-retention", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void prune() {
        retentionService.prune();
    }
}
