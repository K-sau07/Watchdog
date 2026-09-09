package com.watchdog.infrastructure.persistence.posting;

import org.springframework.data.jdbc.repository.query.Modifying;
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

    /** Postings first seen at/after :since, newest first (agent status, D-WD12). */
    @Query("SELECT * FROM posting WHERE first_seen_at >= :since ORDER BY first_seen_at DESC")
    List<PostingRow> findSeenSince(@Param("since") java.time.Instant since);

    /**
     * Delete stale postings (posted before :cutoff) that no user has acted on. NULL
     * posted_at is kept (can't confirm age). Protects any posting with a job_state row
     * (D-WD18). Returns rows deleted.
     */
    @Modifying
    @Query("DELETE FROM posting p WHERE p.posted_at < :cutoff "
            + "AND NOT EXISTS (SELECT 1 FROM job_state js WHERE js.posting_id = p.id)")
    int deleteStalePostedBefore(@Param("cutoff") java.time.Instant cutoff);
}
