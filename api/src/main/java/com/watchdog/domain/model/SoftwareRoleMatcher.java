package com.watchdog.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic "is this a software-engineering role?" classifier for ingest-time filtering
 * (D-WD23/D-WD23b). Pure/stateless, mirrors the other matchers. Defines the coarse
 * superset Watchdog stores — the read-side {@link FilterCriteria} narrows within it.
 *
 * <p><b>Order: EXCLUDE wins.</b> Many non-software roles borrow "engineer"/"architect"
 * ("Solutions Architect", "Sales Engineer", "Developer Advocate"). So a title is a
 * software role only if it hits INCLUDE and does NOT hit EXCLUDE. Tuned against a live
 * audit of the posting table (kept the real software universe, dropped sales/devrel/
 * support/non-software eng).
 */
public final class SoftwareRoleMatcher {

    private SoftwareRoleMatcher() {
    }

    // Comprehensive software-role vocabulary (any match on the title).
    private static final Pattern INCLUDE = Pattern.compile(
            "\\b("
                    + "software engineer|software developer|software development|swe|sde|s\\.d\\.e|"
                    + "back[- ]?end|front[- ]?end|full[- ]?stack|web (developer|engineer)|"
                    + "mobile (engineer|developer)|ios (engineer|developer)|android (engineer|developer)|"
                    + "devops|sre|site reliability|platform engineer|infrastructure engineer|infra engineer|"
                    + "systems engineer|data engineer|machine learning|ml engineer|ai engineer|"
                    + "applied (ai|ml|scientist|science)|deep learning|data scientist|"
                    + "research (engineer|scientist)|security engineer|application security|appsec|"
                    + "cloud engineer|embedded (engineer|software|systems)|firmware|distributed systems|"
                    + "member of technical staff|technical staff|\\bmts\\b|software architect|"
                    + "principal engineer|staff engineer|programmer|forward deployed engineer|\\bfde\\b|"
                    + "agent developer|developer experience engineer|"
                    + "engineer (i|ii|iii|iv|1|2|3|4)|software engineer \\d"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    // Titles that borrow eng words but are NOT software-building roles (drop even if INCLUDE hits).
    private static final Pattern EXCLUDE = Pattern.compile(
            "\\b("
                    + "solutions? (architect|engineer|consultant)|sales engineer|pre[- ]?sales|"
                    + "gtm engineer|customer (engineer|success)|technical services|technical support|"
                    + "support engineer|field engineer|deployment strategist|design engineer|"
                    + "hardware engineer|mechanical|electrical engineer|civil engineer|"
                    + "systems administrator|network engineer|qa (manual|analyst|engineer)|"
                    + "test engineer|quality engineer|account executive|"
                    + "developer (advocate|educator|engagement|relations)|dev ?rel|"
                    + "data annotation|business development|marketing|recruit"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** True if the title is a software-engineering role worth ingesting. */
    public static boolean isSoftwareRole(String title) {
        if (title == null || title.isBlank()) {
            return false; // no title → not confidently software; don't ingest
        }
        String t = title.toLowerCase(Locale.ROOT);
        if (EXCLUDE.matcher(t).find()) {
            return false;
        }
        return INCLUDE.matcher(t).find();
    }
}
