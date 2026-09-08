package com.watchdog.domain.port;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.Posting;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence for {@link Posting}. Implemented in infrastructure
 * (S2, Postgres + Flyway). The dedup contract (spec §8.3) lives here: a posting is
 * uniquely identified by {@code (companyId, atsPostingId)}.
 */
public interface PostingRepository {

    /** Persist a new posting (or update if the natural key already exists). */
    Posting save(Posting posting);

    Optional<Posting> findById(PostingId id);

    /**
     * Dedup lookup: has this exact posting (by natural key) already been seen?
     * Drives "new exactly once" in the agent loop (S4).
     */
    Optional<Posting> findByNaturalKey(CompanyId companyId, String atsPostingId);

    /** True when {@code (companyId, atsPostingId)} already exists — fast dedup check. */
    boolean existsByNaturalKey(CompanyId companyId, String atsPostingId);

    /** All postings for a company (for backfill/inspection; not the filtered feed). */
    List<Posting> findByCompany(CompanyId companyId);

    /**
     * The feed's candidate set: the most-recently-seen postings, newest first (by
     * {@code firstSeenAt} desc), capped at {@code limit}. MatchingService filters and
     * paginates this window in memory (D-WD10, fetch-then-filter for v1). The cap bounds
     * memory at low v1 volume; outgrowing it is the measured trigger to push filters to
     * SQL (Phase 2).
     */
    List<Posting> findRecent(int limit);

    /**
     * Postings first seen at or after {@code since}, newest first. Powers the agent-status
     * "new today" count + "median catch-time today" (spec §7 /api/agent/status, D-WD12
     * DB-derived). Caller passes the start-of-day instant.
     */
    List<Posting> findSeenSince(java.time.Instant since);
}
