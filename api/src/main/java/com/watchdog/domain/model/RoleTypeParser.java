package com.watchdog.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic IC-vs-manager inference from a posting's title. Pure, stateless,
 * deterministic — mirrors {@link SeniorityParser}.
 *
 * <p><b>Why "manager" in the title is not enough.</b> A product manager, program
 * manager, or account manager is an individual contributor; the word names the domain
 * they own, not people reporting to them. Those titles are checked first and claimed for
 * {@link RoleType#IC}, because sending a PM role to someone filtering out management is
 * the more annoying error — it is the single most common title containing "manager".
 *
 * <p><b>Why IC is the default.</b> Engineering titles overwhelmingly describe IC work,
 * and "Staff Engineer" or "Tech Lead" are senior IC posts, not management. Only an
 * explicit management signal flips the answer. {@link RoleType#UNKNOWN} is reserved for
 * a title we cannot read at all.
 */
public final class RoleTypeParser {

    private RoleTypeParser() {
    }

    /**
     * Titles where "manager" names a domain, not direct reports. Checked before
     * {@link #MANAGER} so the generic rule cannot claim them.
     */
    private static final Pattern IC_MANAGER_TITLE = Pattern.compile(
            "\\b(product|program|project|account|community|partner|marketing|category|"
                    + "brand|content|delivery|engagement|success)\\s+manager\\b");

    /** Explicit people-management signals. */
    private static final Pattern MANAGER = Pattern.compile(
            "\\b(manager|director|head\\s+of|vp|vice\\s+president|svp|evp|chief|"
                    + "c[te]o|cpo|ciso|supervisor)\\b");

    /** Infers the role type; never null. */
    public static RoleType parse(String title) {
        if (title == null || title.isBlank()) {
            return RoleType.UNKNOWN;
        }
        String t = title.toLowerCase(Locale.ROOT);
        if (IC_MANAGER_TITLE.matcher(t).find()) {
            return RoleType.IC;
        }
        return MANAGER.matcher(t).find() ? RoleType.MANAGER : RoleType.IC;
    }
}
