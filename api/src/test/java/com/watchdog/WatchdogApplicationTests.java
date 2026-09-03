package com.watchdog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring context loads cleanly on the empty scaffold.
 * Runs under the 'test' profile, which excludes datasource auto-config
 * (no live DB in S0 / CI).
 */
@SpringBootTest
@ActiveProfiles("test")
class WatchdogApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
