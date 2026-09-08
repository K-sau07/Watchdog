package com.watchdog.infrastructure.scheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling + ShedLock (D-WD8) so the poll cycle runs on exactly one
 * instance (spec §3). Gated by {@code watchdog.polling.enabled} (default true) so the
 * scheduler is OFF in tests — tests invoke {@link com.watchdog.domain.port.PollingUseCase}
 * directly and don't need Redis.
 *
 * <p>{@code defaultLockAtMostFor} is a safety net: if an instance dies mid-cycle, the
 * lock auto-expires so the next tick isn't blocked forever.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@ConditionalOnProperty(name = "watchdog.polling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

    @Bean
    LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "watchdog");
    }
}
