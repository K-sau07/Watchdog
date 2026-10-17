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
     * Today's genuine catches: seen since :since, with a known posted_at, from a board
     * whose start we know, and posted at/after that start. The NOT NULL checks are the
     * whole point — a null posted_at or a null first_polled_at is an unknown, and an
     * unknown must not be averaged in as if it were a fast catch.
     */
    @Query("SELECT p.* FROM posting p JOIN company c ON c.id = p.company_id "
            + "WHERE p.first_seen_at >= :since "
            + "AND p.posted_at IS NOT NULL "
            + "AND c.first_polled_at IS NOT NULL "
            + "AND p.posted_at >= c.first_polled_at "
            + "ORDER BY p.first_seen_at DESC")
    List<PostingRow> findCaughtSince(@Param("since") java.time.Instant since);

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
