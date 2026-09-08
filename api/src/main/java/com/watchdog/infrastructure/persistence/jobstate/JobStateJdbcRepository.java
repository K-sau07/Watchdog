package com.watchdog.infrastructure.persistence.jobstate;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JDBC repository for {@link JobStateRow}. Infra-internal. */
interface JobStateJdbcRepository extends CrudRepository<JobStateRow, UUID> {

    Optional<JobStateRow> findByUserIdAndPostingId(UUID userId, UUID postingId);
}
