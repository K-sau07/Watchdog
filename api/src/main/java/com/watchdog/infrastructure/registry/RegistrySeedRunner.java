package com.watchdog.infrastructure.registry;

import com.watchdog.application.registry.DiscoveryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the company registry once at startup (idempotent). Gated by
 * {@code watchdog.registry.seed-on-startup} (default true; set false in the test
 * profile so integration tests control their own data).
 */
@Component
@ConditionalOnProperty(name = "watchdog.registry.seed-on-startup", havingValue = "true", matchIfMissing = true)
public class RegistrySeedRunner implements ApplicationRunner {

    private final DiscoveryService discoveryService;

    public RegistrySeedRunner(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        discoveryService.seed();
    }
}
