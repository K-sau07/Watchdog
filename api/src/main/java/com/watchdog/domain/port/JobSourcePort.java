package com.watchdog.domain.port;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;

import java.util.List;

/**
 * Outbound port: fetch current postings from an ATS board for one company.
 * Implemented per-ATS in infrastructure (S3: GreenhouseClient, LeverClient,
 * AshbyClient). Each implementation declares which {@link AtsSource} it serves so
 * the agent loop can route a company to the right adapter.
 *
 * <p>Contract: returns the postings currently visible on the board, normalized to the
 * domain {@link Posting} shape (with {@code firstSeenAt} set to the observation time).
 * Dedup against already-seen postings is NOT this port's job — that happens downstream
 * (S4). Implementations should surface transport/parse failures as exceptions; the
 * agent loop isolates a single company/source failure so the cycle continues (spec §8.5).
 */
public interface JobSourcePort {

    /** Which ATS this adapter serves. */
    AtsSource source();

    /**
     * Fetch and normalize all postings currently on the company's ATS board.
     *
     * @param company the company to poll (its {@code atsSlug} keys the ATS API)
     * @return normalized postings; empty if the board has none
     */
    List<Posting> fetchPostings(Company company);
}
