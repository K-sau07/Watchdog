package com.watchdog.application.registry;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.FundingStage;
import com.watchdog.domain.model.StageSource;

import java.time.LocalDate;
import java.util.List;

/**
 * Supplies the seed set of companies to register (D-WD1). Implemented in
 * infrastructure (reads the classpath seed file); abstracted here so
 * {@link DiscoveryService} is unit-testable with a fake source.
 */
public interface CompanySeedSource {

    List<SeedEntry> load();

    /**
     * One verified ATS board: display name, source, slug, and what we believe about
     * the company's funding stage.
     *
     * <p>The stage fields are nullable in the file and normalised here, because the
     * seed is generated from a directory that predates stage tracking and because
     * hand-added rows will keep arriving without one.
     *
     * @param matchConfidence how well the generator proved this board belongs to this
     *        company. {@code HOST_VERIFIED} means the board's content links back to
     *        the company's own site; {@code SLUG_NAME_MATCH} means the slug matched
     *        the domain label and the name appeared in the postings, which is usually
     *        right but cannot rule out a same-name collision. Carried through so a bad
     *        row can be found later instead of silently polluting the feed.
     */
    record SeedEntry(
            String name,
            AtsSource source,
            String slug,
            FundingStage stage,
            StageSource stageSource,
            LocalDate stageAsOf,
            String matchConfidence
    ) {

        public SeedEntry {
            stage = stage == null ? FundingStage.UNKNOWN : stage;
            stageSource = stageSource == null ? StageSource.UNKNOWN : stageSource;
        }

        /** A row with no stage claim — the shape the hand-curated seed rows have. */
        public SeedEntry(String name, AtsSource source, String slug) {
            this(name, source, slug, FundingStage.UNKNOWN, StageSource.UNKNOWN, null, null);
        }
    }
}
