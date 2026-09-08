package com.watchdog.domain.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic salary extraction from a posting's body (spec §5, D-WD9 = keyword/regex lists in
 * v1). Pure, stateless, deterministic — mirrors {@link PostingMatcher}.
 *
 * <p><b>When it runs.</b> Only as a fallback when the ATS gave no structured compensation.
 * Greenhouse and Lever never expose structured salary; Ashby sometimes does (that path is
 * already populated upstream in S3). The orchestration layer calls this only when
 * {@link Salary#isEmpty()}.
 *
 * <p><b>Honesty over recall (spec §8.4 "freshness is honest" — same principle for pay).</b>
 * A wrong salary is worse than no salary: it corrupts the salary-floor filter. So this
 * deliberately under-reads. It accepts a figure only with a real money signal — a {@code $},
 * a {@code k} suffix, or an explicit currency code — and only when the amount is a plausible
 * annual figure ({@code >= 10,000}). This rejects headcounts ("500 employees"), founding
 * years ("2015"), and user counts, which a bare-number match would otherwise coerce into
 * salaries. Returns {@link Salary#empty()} whenever nothing parses confidently.
 *
 * <p><b>Known v1 limitations (measure-first, refine later):</b> hourly rates ("$50/hr") are
 * rejected rather than annualized (coercing to 50 would lie); an unmarked range with no money
 * signal ("100,000 to 140,000") is rejected because it is indistinguishable from a
 * non-salary range; the bare {@code $} sets no currency (ambiguous USD/CAD/AUD) — only an
 * explicit code does.
 */
public final class SalaryParser {

    private SalaryParser() {
    }

    private static final BigDecimal MIN_ANNUAL = new BigDecimal("10000");

    // A monetary amount: optional $, digits with comma-grouping or plain, optional k suffix.
    // Two capture groups per amount: (number, kSuffix).
    private static final String AMOUNT = "\\$?\\s*(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*(k)?";

    // A range: amount (— / - / – / to) amount. Groups: 1,2 = low; 3,4 = high.
    private static final Pattern RANGE =
            Pattern.compile(AMOUNT + "\\s*(?:-|–|—|to)\\s*" + AMOUNT, Pattern.CASE_INSENSITIVE);

    // A single amount that MUST carry a '$' (a bare number is too ambiguous alone).
    // Groups: 1 = number, 2 = kSuffix.
    private static final Pattern SINGLE =
            Pattern.compile("\\$\\s*(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*(k)?",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern CURRENCY =
            Pattern.compile("\\b(USD|CAD|GBP|EUR|AUD)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Extract a {@link Salary} from a posting body, or {@link Salary#empty()} when nothing
     * parses confidently. Ranges are preferred; a single $-marked figure becomes a min-only
     * salary.
     */
    public static Salary parse(String description) {
        if (description == null || description.isBlank()) {
            return Salary.empty();
        }
        String text = description.toLowerCase(Locale.ROOT);
        String currency = findCurrency(text);

        Matcher range = RANGE.matcher(text);
        if (range.find()) {
            BigDecimal low = amount(range.group(1), range.group(2));
            BigDecimal high = amount(range.group(3), range.group(4));
            boolean moneySignal = hasDollarOrK(range.group()) || currency != null;
            if (moneySignal && high.compareTo(MIN_ANNUAL) >= 0) {
                return Salary.of(low, high, currency);
            }
        }

        Matcher single = SINGLE.matcher(text);
        if (single.find()) {
            BigDecimal value = amount(single.group(1), single.group(2));
            if (value.compareTo(MIN_ANNUAL) >= 0) {
                return Salary.of(value, null, currency);
            }
        }

        return Salary.empty();
    }

    private static BigDecimal amount(String number, String kSuffix) {
        BigDecimal value = new BigDecimal(number.replace(",", ""));
        return kSuffix == null ? value : value.multiply(new BigDecimal("1000"));
    }

    private static boolean hasDollarOrK(String matched) {
        return matched.indexOf('$') >= 0 || matched.toLowerCase(Locale.ROOT).indexOf('k') >= 0;
    }

    private static String findCurrency(String text) {
        Matcher m = CURRENCY.matcher(text);
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : null;
    }
}
