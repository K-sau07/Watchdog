package com.watchdog.infrastructure.persistence.posting;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JDBC repository for {@link PostingRow}. Infra-internal. */
interface PostingJdbcRepository extends CrudRepository<PostingRow, UUID> {

    Optional<PostingRow> findByCompanyIdAndAtsPostingId(UUID companyId, String atsPostingId);

    boolean existsByCompanyIdAndAtsPostingId(UUID companyId, String atsPostingId);

    List<PostingRow> findByCompanyId(UUID companyId);

    /** Newest-first candidate window for the feed (D-WD10). Verified in S6.6. */
    @Query("SELECT * FROM posting ORDER BY first_seen_at DESC LIMIT :limit")
    List<PostingRow> findRecent(@Param("limit") int limit);
}
