package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.MatchingService;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.infrastructure.web.PostingDtos.PageResponse;
import com.watchdog.infrastructure.web.PostingDtos.PostingDetail;
import com.watchdog.infrastructure.web.PostingDtos.PostingSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dashboard's postings API (spec §7). Thin controller: bind params, delegate to
 * {@link MatchingService}, map to DTOs. All filtering/enrichment/pagination lives in the
 * service; no business logic here.
 *
 * <ul>
 *   <li>{@code GET /api/postings} — filtered, paginated feed, newest first.</li>
 *   <li>{@code GET /api/postings/{id}} — full detail incl. description.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/postings")
class PostingController {

    private final MatchingService matching;

    PostingController(MatchingService matching) {
        this.matching = matching;
    }

    @GetMapping
    public PageResponse<PostingSummary> list(PostingQueryParams params) {
        FilterCriteria criteria = PostingQueryMapper.toCriteria(params);
        int page = params.pageOrDefault();
        int size = params.sizeOrDefault();

        MatchingService.Page result = matching.search(criteria, page, size);
        List<PostingSummary> items = result.items().stream()
                .map(PostingDtos::toSummary)
                .toList();
        return new PageResponse<>(items, result.page(), result.size(),
                result.totalMatched(), result.totalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostingDetail> detail(@PathVariable String id) {
        PostingId postingId = parseId(id);
        return matching.findEnriched(postingId)
                .map(PostingDtos::toDetail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static PostingId parseId(String raw) {
        try {
            return PostingId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new PostingQueryMapper.BadFilterParam("invalid posting id: '" + raw + "'");
        }
    }

    /** A malformed filter or id is a client error → 400 with a short reason. */
    @ExceptionHandler(PostingQueryMapper.BadFilterParam.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onBadParam(PostingQueryMapper.BadFilterParam e) {
        return Map.of("error", e.getMessage());
    }
}
