package com.watchdog.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic seniority inference from a posting's title (spec §5, D-WD9 = keyword/regex
 * lists in v1). Pure, stateless, deterministic — mirrors {@link PostingMatcher}.
 *
 * <p><b>Why title-first.</b> The title is the reliable seniority signal; bodies mention
 * "senior" in unrelated contexts ("work with senior engineers"). The body is consulted
 * only as a fallback for the early-career signals ("new grad" / "n years experience"),
 * which titles often omit even when the role is junior.
 *
 * <p><b>Precedence: most senior wins, then most specific.</b> Titles like "Senior Software
 * Engineer, New Grad Program" must not read as NEW_GRAD. Rules are evaluated STAFF/PRINCIPAL
 * → SENIOR → MID → INTERN → NEW_GRAD → JUNIOR and the first hit wins. Two deliberate ordering
 * calls: (1) a senior signal always dominates a junior one in the same title; (2) among
 * early-career signals the more specific INTERN and NEW_GRAD win over the generic JUNIOR
 * ("associate"/"entry-level"), because mislabelling a new-grad role as JUNIOR would hide it
 * from the product's default new-grad hunt. Honest {@link Seniority#UNKNOWN} when nothing
 * matches (spec §5 "good enough, not perfect").
 */
public final class SeniorityParser {

    private SeniorityParser() {
    }

    // Word-boundary patterns so "seniority" != "senior", "internal" != "intern".
    // Ordered most-senior → least; first match wins.
    private static final Pattern STAFF =
            Pattern.compile("\\b(staff|principal|distinguished|fellow)\\b");
    private static final Pattern SENIOR =
            Pattern.compile("\\b(senior|sr\\.?|lead|architect)\\b");
    // Mid: explicit level markers or an experience floor of 2+ years.
    // Level markers ("II"/"III") require a preceding space so we don't match stray letters.
    private static final Pattern MID =
            Pattern.compile("\\b(mid|intermediate)\\b| (ii|iii)\\b|\\b([2-9]\\+?\\s*years)\\b");
    // Junior: named junior signals + a trailing "I" level marker (space-anchored, and only
    // after MID has already claimed "II"/"III").
    private static final Pattern JUNIOR =
            Pattern.compile("\\b(junior|jr\\.?|associate|entry[\\s-]?level|early[\\s-]?career)\\b| i\\b");
    private static final Pattern NEW_GRAD =
            Pattern.compile("\\b(new[\\s-]?grad(uate)?|university[\\s-]?grad(uate)?|campus|"
                    + "recent[\\s-]?grad(uate)?|early[\\s-]?talent|apprentice)\\b");
    private static final Pattern INTERN =
            Pattern.compile("\\b(intern|internship|co[\\s-]?op|summer\\s+20\\d{2})\\b");

    // Body-only fallback: early-career signals titles often omit.
    private static final Pattern BODY_NEW_GRAD =
            Pattern.compile("\\b(new[\\s-]?grad(uate)?|recent[\\s-]?graduate|"
                    + "0[\\s-]?(to|-)?\\s*[12]\\s*years|no\\s+prior\\s+experience)\\b");


    /**
     * Infer seniority from a title, consulting the body only as a fallback for early-career
     * signals. Returns {@link Seniority#UNKNOWN} when no rule fires.
     *
     * @param title       the posting title (required signal); blank/null → body-only, then UNKNOWN
     * @param description the posting body (may be null); used only for the new-grad fallback
     */
    public static Seniority parse(String title, String description) {
        String t = lower(title);

        // Staff/principal and senior/lead both roll up to the SENIOR bucket (the enum has no
        // STAFF value — see class javadoc); kept as two patterns because the token sets differ.
        if (STAFF.matcher(t).find() || SENIOR.matcher(t).find()) return Seniority.SENIOR;
        if (MID.matcher(t).find()) return Seniority.MID;
        if (INTERN.matcher(t).find()) return Seniority.INTERN;
        if (NEW_GRAD.matcher(t).find()) return Seniority.NEW_GRAD;
        if (JUNIOR.matcher(t).find()) return Seniority.JUNIOR;

        // Title gave no signal — fall back to explicit early-career markers in the body.
        String d = lower(description);
        if (BODY_NEW_GRAD.matcher(d).find()) return Seniority.NEW_GRAD;

        return Seniority.UNKNOWN;
    }

    /** Convenience overload for a title-only inference. */
    public static Seniority parse(String title) {
        return parse(title, null);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
