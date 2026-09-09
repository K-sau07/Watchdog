package com.watchdog.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic "is this location in the United States?" classifier (D-WD17), for the
 * US-only filter. Pure/stateless. Location data is messy free-text, so this is best-effort.
 *
 * <p><b>Order matters (US-positive first).</b> A posting can list several locations
 * ("Remote, Canada; Remote, United States") — that IS a US-available job, so a positive US
 * signal wins over a co-present foreign one. So: check US signals first (keep), then known
 * foreign signals (drop), then default-keep for ambiguous ("Remote", "Hybrid", null) —
 * inclusive per D-WD17 (a false-include you skip beats a false-exclude you never see).
 *
 * <p>US signals: "United States"/"USA"/"US", "North America", all 50 state names + DC,
 * ", CA"-style abbreviations, and major US cities. Foreign signals: a broad list of
 * countries, foreign cities, and regions. Both lists are heuristic and grow as real data
 * reveals gaps (this class was tuned against a live audit of the posting table).
 */
public final class UsLocationClassifier {

    private UsLocationClassifier() {
    }

    // US signals (checked FIRST). State names + "US"/"USA"/"North America"/"NYC".
    private static final Pattern US = Pattern.compile(
            "\\b(united states|u\\.?s\\.?a|u\\.?s\\.?|north america|nyc|"
                    + "alabama|alaska|arizona|arkansas|california|colorado|connecticut|delaware|"
                    + "florida|georgia|hawaii|idaho|illinois|indiana|iowa|kansas|kentucky|"
                    + "louisiana|maine|maryland|massachusetts|michigan|minnesota|mississippi|"
                    + "missouri|montana|nebraska|nevada|new hampshire|new jersey|new mexico|"
                    + "new york|north carolina|north dakota|ohio|oklahoma|oregon|pennsylvania|"
                    + "rhode island|south carolina|south dakota|tennessee|texas|utah|vermont|"
                    + "virginia|washington|west virginia|wisconsin|wyoming)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> US_CITIES = List.of(
            "san francisco", "new york", "seattle", "austin", "boston", "chicago",
            "los angeles", "denver", "atlanta", "mountain view", "foster city", "palo alto",
            "sunnyvale", "san jose", "san diego", "portland", "miami", "dallas", "houston",
            "philadelphia", "phoenix", "bellevue", "cambridge", "brooklyn", "menlo park",
            "redwood city", "santa clara", "irvine", "nashville", "salt lake", "raleigh",
            "durham", "pittsburgh", "minneapolis", "detroit", "kansas city", "mclean");

    private static final Pattern US_STATE_ABBR = Pattern.compile(
            ",\\s*(AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|"
                    + "MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|"
                    + "WV|WI|WY|DC)\\b");

    // Known non-US signals (countries, foreign cities/regions). Checked AFTER US signals.
    private static final Pattern FOREIGN = Pattern.compile(
            "\\b(india|singapore|canada|united kingdom|u\\.?k\\.?|england|scotland|wales|"
                    + "ireland|dublin|london|japan|tokyo|france|paris|germany|berlin|munich|"
                    + "netherlands|amsterdam|spain|madrid|barcelona|poland|krakow|warsaw|"
                    + "brazil|s\\u00e3o paulo|sao paulo|argentina|australia|sydney|melbourne|"
                    + "new zealand|bengaluru|bangalore|mumbai|delhi|hyderabad|pune|chennai|"
                    + "gurgaon|noida|vancouver|toronto|montreal|ottawa|ontario|british columbia|"
                    + "alberta|quebec|mexico city|switzerland|z\\u00fcrich|zurich|geneva|sweden|"
                    + "stockholm|norway|oslo|denmark|copenhagen|aarhus|finland|helsinki|iceland|"
                    + "reykjav|portugal|lisbon|porto|italy|milan|rome|austria|vienna|czech|"
                    + "prague|hungary|budapest|serbia|belgrade|slovenia|ljubljana|romania|"
                    + "bucharest|bulgaria|greece|athens|croatia|slovakia|estonia|latvia|"
                    + "lithuania|malta|luxembourg|belgium|brussels|cyprus|dubai|abu dhabi|uae|"
                    + "ksa|saudi|qatar|kuwait|bahrain|oman|tel aviv|israel|philippines|manila|"
                    + "vietnam|hanoi|indonesia|jakarta|thailand|bangkok|malaysia|korea|seoul|"
                    + "china|beijing|shanghai|hong kong|taiwan|emea|apac|latam|europe|"
                    + "costa rica|colombia|chile|peru|uruguay|egypt|morocco|nigeria|ghana|"
                    + "kenya|south africa|turkey|ukraine)\\b",
            Pattern.CASE_INSENSITIVE);

    /** True if the location should pass a "United States only" filter (inclusive). */
    public static boolean isUnitedStates(String location) {
        if (location == null || location.isBlank()) {
            return true; // ambiguous → keep (inclusive)
        }
        String lower = location.toLowerCase(Locale.ROOT);

        // US-positive first: a US signal wins even if a foreign token is also present
        // (e.g. "Remote, Canada; Remote, United States" is a US-available role).
        if (US.matcher(lower).find()) return true;
        if (US_STATE_ABBR.matcher(location).find()) return true; // original case for abbr
        for (String city : US_CITIES) {
            if (lower.contains(city)) return true;
        }

        if (FOREIGN.matcher(lower).find()) return false; // foreign-only → drop

        return true; // ambiguous / unknown → keep (inclusive, D-WD17)
    }
}
