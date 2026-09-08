package com.watchdog.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Compensation as exposed by some ATS boards (spec §4/§5). All fields optional —
 * many postings expose none, a min only, or a full range. Monetary amounts use
 * BigDecimal; currency is an ISO-4217-ish code as the ATS provides it (not validated
 * here — heuristic parsing lives in S6).
 *
 * <p>A Salary with no data at all is represented by {@link #empty()} rather than null,
 * so a Posting always has a non-null Salary.
 */
public record Salary(BigDecimal min, BigDecimal max, String currency) {

    private static final Salary EMPTY = new Salary(null, null, null);

    /** The "no compensation data" value. */
    public static Salary empty() {
        return EMPTY;
    }

    public static Salary of(BigDecimal min, BigDecimal max, String currency) {
        return new Salary(min, max, currency);
    }

    public Optional<BigDecimal> minOpt() {
        return Optional.ofNullable(min);
    }

    public Optional<BigDecimal> maxOpt() {
        return Optional.ofNullable(max);
    }

    public Optional<String> currencyOpt() {
        return Optional.ofNullable(currency);
    }

    /** True when the ATS gave us no compensation signal at all. */
    public boolean isEmpty() {
        return min == null && max == null && currency == null;
    }
}
