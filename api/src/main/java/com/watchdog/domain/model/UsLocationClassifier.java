package com.watchdog.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic "is this location in the United States?" classifier (D-WD17), for the
 * US-only filter. Pure/stateless, mirrors the other parsers. Location data is messy
 * free-text ("San Francisco", "Remote - USA", "Singapore", "Remote Canada", bare
 * "Remote", null), so this is a best-effort heuristic, not ground truth.
 *
 * <p><b>Inclusive policy (D-WD17):</b> ordered — foreign signals first, then US signals,
 * then default-keep. Ambiguous locations ("Remote", "Hybrid", "Distributed", null) are
 * KEPT: for a job hunter a false-include you can skip beats a false-exclude you never
 * see. Foreign is checked first so "Remote Canada" is dropped before any remote-keep.
 */
public final class UsLocationClassifier {

    private UsLocationClassifier() {
    }

    // Known non-US signals (countries, foreign cities, foreign regions). Checked FIRST.
    private static final Pattern FOREIGN = Pattern.compile(
            "\\b(india|singapore|canada|united kingdom|england|scotland|ireland|dublin|"
                    + "london|japan|tokyo|france|paris|germany|berlin|munich|netherlands|amsterdam|"
                    + "spain|madrid|barcelona|poland|krakow|warsaw|brazil|mexico|argentina|"
                    + "australia|sydney|melbourne|new zealand|bengaluru|bangalore|mumbai|delhi|"
                    + "hyderabad|pune|chennai|gurgaon|noida|vancouver|toronto|montreal|ottawa|"
                    + "ontario|british columbia|alberta|quebec|zurich|geneva|stockholm|oslo|"
                    + "copenhagen|helsinki|lisbon|porto|milan|rome|vienna|prague|budapest|"
                    + "dubai|abu dhabi|tel aviv|israel|philippines|manila|vietnam|hanoi|"
                    + "indonesia|jakarta|thailand|bangkok|malaysia|korea|seoul|china|beijing|"
                    + "shanghai|hong kong|taiwan|emea|apac|latam|united arab emirates)\\b",
            Pattern.CASE_INSENSITIVE);

    // Explicit US phrases.
    private static final Pattern US_PHRASE = Pattern.compile(
            "\\b(united states|u\\.?s\\.?a|remote\\s*[-,]?\\s*us|us\\s+remote|usa)\\b",
            Pattern.CASE_INSENSITIVE);

    // Major US cities (the common ones in real ATS data; not exhaustive).
    private static final List<String> US_CITIES = List.of(
            "san francisco", "new york", "seattle", "austin", "boston", "chicago",
            "los angeles", "denver", "atlanta", "mountain view", "foster city", "palo alto",
            "sunnyvale", "san jose", "san diego", "washington", "portland", "miami", "dallas",
            "houston", "philadelphia", "phoenix", "bellevue", "cambridge", "brooklyn",
            "menlo park", "redwood city", "santa clara", "irvine", "nashville", "salt lake",
            "raleigh", "durham", "pittsburgh", "minneapolis", "detroit", "kansas city");

    // ", CA" / ", NY" style state abbreviations (case-sensitive on the abbr to avoid noise).
    private static final Pattern US_STATE_ABBR = Pattern.compile(
            ",\\s*(AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|"
                    + "MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|"
                    + "WV|WI|WY|DC)\\b");

    /** True if the location should pass a "United States only" filter (inclusive). */
    public static boolean isUnitedStates(String location) {
        if (location == null || location.isBlank()) {
            return true; // ambiguous → keep (inclusive)
        }
        String lower = location.toLowerCase(Locale.ROOT);

        if (FOREIGN.matcher(lower).find()) return false;          // foreign first
        if (US_PHRASE.matcher(lower).find()) return true;         // explicit US
        for (String city : US_CITIES) {                           // US city
            if (lower.contains(city)) return true;
        }
        if (US_STATE_ABBR.matcher(location).find()) return true;  // ", CA" (original case)

        return true; // ambiguous / unknown → keep (inclusive, D-WD17)
    }
}
