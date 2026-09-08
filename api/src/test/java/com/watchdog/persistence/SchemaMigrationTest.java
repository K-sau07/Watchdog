package com.watchdog.persistence;

import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Flyway V1 migration applies against real Postgres and creates the
 * expected schema. This is the S2.1 harness smoke test.
 */
class SchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void allExpectedTablesExist() {
        List<String> tables = jdbc.sql(
                        "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
                .query(String.class)
                .list();

        assertThat(tables).contains(
                "company", "posting", "app_user", "job_state", "filter_profile");
    }

    @Test
    void flywayRecordedTheMigration() {
        Integer applied = jdbc.sql(
                        "SELECT count(*) FROM flyway_schema_history WHERE success = true")
                .query(Integer.class)
                .single();
        assertThat(applied).isGreaterThanOrEqualTo(1);
    }

    @Test
    void postingDedupIndexIsUnique() {
        // The natural-key unique constraint must exist for dedup (spec §8.3).
        Long count = jdbc.sql(
                        "SELECT count(*) FROM pg_indexes " +
                        "WHERE tablename = 'posting' AND indexname = 'uq_posting_natural_key'")
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(1L);
    }
}
