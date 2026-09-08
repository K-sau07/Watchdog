package com.watchdog.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic visa/sponsorship inference from a posting's body (spec §5, D-WD9 =
 * keyword/regex lists in v1). Pure, stateless, deterministic — mirrors {@link PostingMatcher}.
 *
 * <p><b>Why body-first.</b> Titles essentially never state visa status; the signal lives in
 * the description ("we sponsor visas" / "must be a US citizen"). Both title and body are
 * searched (title is cheap and occasionally carries e.g. "(US Citizens Only)"), but the body
 * is where this almost always resolves.
 *
 * <p><b>Conflict rule: NOT_OFFERED wins.</b> A posting can contain both "sponsorship
 * available" boilerplate and a hard "US citizenship required" clause. For a visa-dependent
 * hunter, a false OFFERED is the costly error (wasted application, false hope), so any
 * genuine no-sponsorship signal dominates a yes-sponsorship one. Honest
 * {@link SponsorshipSignal#UNKNOWN} when neither side fires — the common case, and the spec
 * is explicit this is heuristic in v1 ("refined later").
 */
public final class SponsorshipParser {

    private SponsorshipParser() {
    }

    // --- NOT_OFFERED: citizenship/clearance requirements + explicit no-sponsorship clauses.
    // Checked first and wins on conflict. Kept deliberately specific to avoid firing on
    // neutral mentions ("candidates of all backgrounds").
    private static final Pattern NOT_OFFERED = Pattern.compile(
            "\\bu\\.?s\\.?\\s*citizen(ship|s)?\\b"
                    + "|\\bmust be a citizen\\b"
                    + "|\\bsecurity clearance\\b"
                    + "|\\b(active |ts/sci|top secret)\\s*clearance\\b"
                    + "|\\bno( visa)? sponsorship\\b"
                    + "|\\b(not|unable|cannot|can't|do(es)? not)\\b[^.\\n]{0,30}\\bsponsor(ship)?\\b"
                    + "|\\bwithout (visa )?sponsorship\\b"
                    + "|\\bnot able to sponsor\\b"
                    + "|\\bsponsorship is not (available|offered|provided)\\b"
                    + "|\\bmust be (authorized|eligible) to work[^.\\n]{0,40}without sponsorship\\b");

    // --- OFFERED: affirmative sponsorship language only. Each alternative pairs a positive
    // verb/adjective with "sponsor", so it does not fire on a bare "sponsorship" that a
    // NOT_OFFERED clause already negated (and NOT_OFFERED wins anyway on genuine conflict).
    private static final Pattern OFFERED = Pattern.compile(
            "\\b(will|can|happy to|able to|do|we) sponsor\\b"
                    + "|\\b(visa )?sponsorship (is )?(available|offered|provided)\\b"
                    + "|\\bwe (offer|provide) (visa )?sponsorship\\b"
                    + "|\\b(h-?1b|opt|cpt|green card) sponsorship\\b"
                    + "|\\bimmigration support\\b");


    /**
     * Infer the sponsorship signal from a posting's title + description. NOT_OFFERED is
     * evaluated first and wins on conflict; OFFERED only when no no-sponsorship signal is
     * present; otherwise {@link SponsorshipSignal#UNKNOWN}.
     *
     * @param title       the posting title (may be null)
     * @param description the posting body (may be null) — the primary signal source
     */
    public static SponsorshipSignal parse(String title, String description) {
        String hay = lower(title) + "\n" + lower(description);

        if (NOT_OFFERED.matcher(hay).find()) return SponsorshipSignal.NOT_OFFERED;
        if (OFFERED.matcher(hay).find()) return SponsorshipSignal.OFFERED;
        return SponsorshipSignal.UNKNOWN;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
