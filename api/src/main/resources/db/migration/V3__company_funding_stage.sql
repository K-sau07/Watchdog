-- Funding stage on company (D-WD18).
--
-- The registry used to be 134 hand-picked boards weighted toward late-stage and
-- public companies. It is now seeded from the YC directory, where most rows are
-- early- and growth-stage, and the feed needs to be able to say so.
--
-- Three columns, not one: the claim, its provenance, and its date. Stage decays,
-- and the sources differ in trustworthiness (an inference from YC's coarse
-- Early/Growth flag is not the same as a licensed funding API). Storing the value
-- alone would make a stale guess indistinguishable from a fresh fact.

ALTER TABLE company
    ADD COLUMN funding_stage TEXT NOT NULL DEFAULT 'UNKNOWN'
        CHECK (funding_stage IN ('SEED', 'SERIES_A', 'SERIES_B_PLUS', 'GROWTH', 'UNKNOWN')),
    ADD COLUMN stage_source  TEXT NOT NULL DEFAULT 'UNKNOWN'
        CHECK (stage_source IN ('YC_DIRECTORY', 'MANUAL', 'FUNDING_API', 'UNKNOWN')),
    ADD COLUMN stage_as_of   DATE;

-- The feed's default view is "startup-window companies that are active", so the
-- partial index matches that predicate rather than indexing every row.
CREATE INDEX idx_company_stage_active
    ON company (funding_stage)
    WHERE active AND funding_stage IN ('SEED', 'SERIES_A', 'SERIES_B_PLUS');

COMMENT ON COLUMN company.funding_stage IS
    'Funding stage. Mostly inferred from the YC directory''s Early/Growth flag plus '
    'team size, not an observed funding fact — see stage_source.';
COMMENT ON COLUMN company.stage_source IS
    'Provenance of funding_stage. Ranked YC_DIRECTORY < MANUAL < FUNDING_API; a '
    'refresh only overwrites a claim from a lower-ranked source.';
COMMENT ON COLUMN company.stage_as_of IS
    'When the stage claim was made. Null when no claim has been recorded.';
