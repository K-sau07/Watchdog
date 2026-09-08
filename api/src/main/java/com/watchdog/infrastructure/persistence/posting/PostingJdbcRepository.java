package com.watchdog.infrastructure.persistence.posting;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JDBC repository for {@link PostingRow}. Infra-internal. */
interface PostingJdbcRepository extends CrudRepository<PostingRow, UUID> {

    Optional<PostingRow> findByCompanyIdAndAtsPostingId(UUID companyId, String atsPostingId);

    boolean existsByCompanyIdAndAtsPostingId(UUID companyId, String atsPostingId);

    List<PostingRow> findByCompanyId(UUID companyId);
}
