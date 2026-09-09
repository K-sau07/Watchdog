package com.watchdog.infrastructure.web;

/**
 * Raw {@code /api/postings} query parameters, bound from the request by Spring and
 * translated to a {@link com.watchdog.domain.model.FilterCriteria} by
 * {@link PostingQueryMapper}. Every field is optional; an absent field means "no
 * constraint on this dimension" (spec §5). List-style fields are comma-separated.
 *
 * <p>Field names are the query-param names: {@code ?roles=&keywords=&exclude=&location=
 * &seniority=&remote=&employmentType=&source=&state=&salaryMin=&includeUnknownSalary=
 * &sponsorship=&postedWithin=&dateFrom=&dateTo=&sort=&page=&size=} (spec §7).
 */
public record PostingQueryParams(
        String roles,
        String keywords,
        String exclude,
        String location,
        String seniority,
        String remote,
        String employmentType,
        String source,
        String state,
        String salaryMin,
        Boolean includeUnknownSalary,
        Boolean usOnly,
        String sponsorship,
        String postedWithin,
        String dateFrom,
        String dateTo,
        Integer page,
        Integer size
) {

    /** Default page index when the client omits it. */
    public int pageOrDefault() {
        return page == null || page < 0 ? 0 : page;
    }

    /** Default page size when the client omits it, clamped to a sane maximum. */
    public int sizeOrDefault() {
        if (size == null || size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }

    static final int DEFAULT_SIZE = 25;
    static final int MAX_SIZE = 100;
}
