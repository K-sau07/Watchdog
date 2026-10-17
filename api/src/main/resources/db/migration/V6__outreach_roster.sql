-- The roster and the touch log.
--
-- V5 recorded that a person was reached out to. That is a fact about a moment, and the
-- work is a sequence: invite, then a DM after they accept, then an email because the DM
-- went nowhere, then a chase two days later. One row with one timestamp cannot answer
-- "have we emailed this person" or "who is owed a follow-up today", so the events get
-- their own table and the contact's counters become a projection over them.
--
-- Why a log rather than more columns on contact: stage, turns and unanswered are also
-- held in the frontmatter of Pipeline/people/*.md. Two copies of one fact is exactly
-- what produced forty-five rows of stale state in the system that vault replaced. Here
-- the events are the only thing written, the counters are derived from them, and the
-- vault keeps the prose it is actually better at.

ALTER TABLE contact
    ADD COLUMN email          TEXT,

    -- Mirrors the `type` field in Pipeline/people/_TEMPLATE.md so the app and the vault
    -- use one vocabulary. ROUTER.md decides this before anything is drafted, because the
    -- label sets the register and the ask: a hiring manager interviews and never refers,
    -- a recruiter routes and has no referral concept at all.
    ADD COLUMN person_type    TEXT CHECK (person_type IN
                                  ('ENGINEER','RECRUITER','HIRING_MANAGER','ALUM','SENIOR_OFF_REQ')),

    -- Stages a human declares rather than earns. Nothing in the event log can tell you
    -- that an ask was made or that someone got irritated, so those are stated, and the
    -- projection respects them over anything it would otherwise derive.
    ADD COLUMN stage_override TEXT CHECK (stage_override IN ('ASKED','WON','DEAD','NURTURE'));

COMMENT ON COLUMN contact.person_type IS
    'Router classification. Same vocabulary as Pipeline/people/_TEMPLATE.md type field.';
COMMENT ON COLUMN contact.stage_override IS
    'A declared stage. Null means the stage is derived from outreach_event.';

CREATE TABLE outreach_event (
    id          UUID PRIMARY KEY,
    contact_id  UUID NOT NULL REFERENCES contact (id) ON DELETE CASCADE,

    direction   TEXT NOT NULL CHECK (direction IN ('OUTBOUND','INBOUND')),

    -- An invite is its own channel, not a flavour of DM, because STAGES.md is explicit
    -- that "an invite note is not a message, it is an invite" and the unanswered counter
    -- must not charge for it.
    channel     TEXT NOT NULL CHECK (channel IN ('LINKEDIN_INVITE','LINKEDIN_DM','EMAIL')),

    kind        TEXT NOT NULL CHECK (kind IN ('INVITE','OPENER','FOLLOW_UP','ASK','REPLY')),

    -- DRAFTED means written but not sent. Only SENT counts against a cap or starts a
    -- follow-up clock: a draft sitting in the app has not reached anybody.
    status      TEXT NOT NULL DEFAULT 'SENT' CHECK (status IN ('DRAFTED','SENT')),

    -- The message itself. A follow-up that cannot see what was already said will repeat
    -- it, which is the single most obvious way to look automated.
    body        TEXT,

    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The projection replays one contact's events newest-first.
CREATE INDEX idx_event_contact ON outreach_event (contact_id, occurred_at DESC);

-- "Invites burned this week" counts over this; partial because inbound replies and
-- unsent drafts are irrelevant to a send cap.
CREATE INDEX idx_event_weekly ON outreach_event (occurred_at)
    WHERE direction = 'OUTBOUND' AND status = 'SENT';

COMMENT ON TABLE outreach_event IS
    'Append-only log of outreach touches and replies. The source of truth for a '
    'contact''s stage, turn count and unanswered counters, which are projections of it.';

-- Backfill: every V5 contact already represents one approach that was made. Without a
-- seed event the projection would read them as never contacted and the board would lose
-- them. Invite channel, because that is what a recorded reach-out was.
INSERT INTO outreach_event (id, contact_id, direction, channel, kind, status, occurred_at)
SELECT gen_random_uuid(), id, 'OUTBOUND', 'LINKEDIN_INVITE', 'INVITE', 'SENT', reached_out_at
FROM contact;

-- A declared stage cannot be derived from events, so carry those across before the
-- column goes. REACHED_OUT, LIVE and EXCHANGE are all derivable and are not carried.
UPDATE contact SET stage_override = stage WHERE stage IN ('ASKED','WON','DEAD','NURTURE');

-- Now drop the projected columns.
--
-- These were V5's stored stage, turn count and unanswered counter. They are computed
-- from outreach_event as of this migration, and leaving them in place would recreate
-- the exact problem this table exists to solve: a second copy of a fact, updated by a
-- different code path, free to disagree with the first. The cap logic that maintained
-- them lived on the Contact record and now lives in OutreachProjection, in one place.
ALTER TABLE contact
    DROP COLUMN stage,
    DROP COLUMN turns,
    DROP COLUMN unanswered;
