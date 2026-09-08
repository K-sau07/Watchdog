package com.watchdog;

import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the full Spring context loads cleanly against a real Postgres
 * container (via {@link PostgresIntegrationTest}) with Flyway applied.
 */
class WatchdogApplicationTests extends PostgresIntegrationTest {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
