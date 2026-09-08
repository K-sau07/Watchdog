package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.MatchingService;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.Salary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTOs for the postings feed (spec §7). Kept separate from the domain
 * {@link Posting} so the wire contract is explicit and stable. All derived heuristic
 * values (seniority, sponsorship, salary) and the signature stat ("caught N min after
 * posting", spec §6) are computed in {@link MatchingService} and surfaced here.
 */
final class PostingDtos {

    private PostingDtos() {
    }

    /** A card in the radar feed. */
    record PostingSummary(
            String id,
            String title,
            String location,
            String remoteType,
            String employmentType,
            String seniority,
            String sponsorshipSignal,
            SalaryDto salary,
            String url,
            String source,          // resolved from the company, may be null if unknown
            Instant postedAt,
            Instant firstSeenAt,
            Long caughtMinutes      // the signature stat; null when postedAt unknown (§8.4)
    ) {
    }

    /** Full detail view — summary fields plus the description. */
    record PostingDetail(
            PostingSummary summary,
            String description
    ) {
    }

    record SalaryDto(BigDecimal min, BigDecimal max, String currency) {
        static SalaryDto from(Salary s) {
            return s.isEmpty() ? null : new SalaryDto(s.min(), s.max(), s.currency());
        }
    }

    /** Generic paginated envelope. */
    record PageResponse<T>(
            List<T> items,
            int page,
            int size,
            int totalMatched,
            int totalPages
    ) {
    }

    // --- mapping ---

    static PostingSummary toSummary(MatchingService.MatchedPosting m) {
        Posting p = m.posting();
        return new PostingSummary(
                p.id().value().toString(),
                p.title(),
                p.location(),
                p.remoteType().name(),
                p.employmentType().name(),
                m.seniority().name(),
                p.sponsorshipSignal().name(),
                SalaryDto.from(p.salary()),
                p.url(),
                m.source() == null ? null : m.source().name(),
                p.postedAt(),
                p.firstSeenAt(),
                p.catchMinutes().orElse(null));
    }

    static PostingDetail toDetail(MatchingService.MatchedPosting m) {
        return new PostingDetail(toSummary(m), m.posting().description());
    }
}
