package com.watchdog.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for persistence integration tests. Uses the Testcontainers <em>singleton</em>
 * pattern: one Postgres 17 container started once for the whole test run and never
 * explicitly stopped (Ryuk / the JVM reaps it at exit). This is deliberate — the
 * JUnit {@code @Testcontainers}/{@code @Container} lifecycle stops the container when
 * the first test class finishes, which breaks subsequent classes that share a base.
 *
 * <p>{@link ServiceConnection} auto-wires the datasource from the running container;
 * Flyway migrates V1 on context startup.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
