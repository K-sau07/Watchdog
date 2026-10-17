-- The reach-out layer.
--
-- SaurabhBrain/Pipeline/core/STAGES.md deliberately tracks nobody until they reply:
-- "No rows for invites. No rows for accepts. LinkedIn holds all of that already and
-- holds it better." That refusal was earned — hand-maintaining a parallel copy of
-- LinkedIn's state produced forty-five rows of stale nonsense in the system before it.
--
-- This table is allowed on different terms. A row is written once, by the app, at the
-- moment a reach-out is sent, from the posting that prompted it. Nothing is re-typed
-- and nothing has to be kept in sync by hand, so the failure mode that doc is guarding
-- against does not apply. What it still refuses is tracking whether they *accepted* —
-- that is LinkedIn's to know, and there is no column for it here.
--
-- Everything from LIVE onward mirrors the markdown files, which stay the source of
-- truth for real conversations.

CREATE TABLE contact (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,

    -- The company tag. Free text rather than a company FK on purpose: people are
    -- approached at companies whose boards we do not watch, and a reach-out must not
    -- be impossible to record just because the company is not in the registry.
    company_name    TEXT NOT NULL,

    -- The posting that prompted it, when there was one. ON DELETE SET NULL because
    -- retention prunes stale postings (D-WD18) and losing a job ad must never delete
    -- the record of a conversation.
    posting_id      UUID REFERENCES posting (id) ON DELETE SET NULL,

    person_name     TEXT NOT NULL,
    person_title    TEXT,
    linkedin_url    TEXT,

    stage           TEXT NOT NULL DEFAULT 'REACHED_OUT'
                        CHECK (stage IN ('REACHED_OUT','LIVE','EXCHANGE','ASKED','WON','NURTURE','DEAD')),

    -- Real back-and-forths. Zero until they write something that is not a pleasantry.
    turns           INTEGER NOT NULL DEFAULT 0 CHECK (turns >= 0),

    -- Consecutive unanswered messages of yours. Two is the cap and it is a refusal,
    -- not a warning. A reply resets this to zero — that reset is why the cap counts
    -- unanswered messages rather than total ones, which is what made a real
    -- conversation illegal under the old three-touch rule.
    unanswered      INTEGER NOT NULL DEFAULT 0 CHECK (unanswered >= 0),

    can_give        TEXT CHECK (can_give IN ('REFER','INTERVIEW','ROUTE')),

    reached_out_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_contact_at TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- One human, one row. Mirrors the one-human-one-file rule in the vault.
    CONSTRAINT uq_contact_user_person UNIQUE (user_id, company_name, person_name)
);

-- The board reads every contact for a user and groups by company in one pass.
CREATE INDEX idx_contact_user_company ON contact (user_id, company_name);

COMMENT ON TABLE contact IS
    'People reached out to, grouped by company. Written at send time by the app; '
    'stages past REACHED_OUT mirror the Pipeline vault markdown.';
COMMENT ON COLUMN contact.unanswered IS
    'Consecutive unanswered messages. 2 means stop DMing (comments only). A reply '
    'resets it to 0.';
