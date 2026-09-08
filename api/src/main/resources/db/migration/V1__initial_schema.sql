-- Watchdog initial schema (spec §4).
-- Conventions:
--   * PKs are application-generated UUIDs (domain mints them; no DB default).
--   * Enums stored as text + CHECK (easy to evolve vs native PG enums);
--     values match Java enum constant names exactly.
--   * Timestamps are timestamptz (maps to java.time.Instant).
--   * Dedup + per-user uniqueness enforced by unique indexes.

-- ── company ──────────────────────────────────────────────────────────────
CREATE TABLE company (
    id             UUID PRIMARY KEY,
    name           TEXT NOT NULL,
    ats_source     TEXT NOT NULL CHECK (ats_source IN ('GREENHOUSE', 'LEVER', 'ASHBY')),
    ats_slug       TEXT NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    last_polled_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_company_source_slug UNIQUE (ats_source, ats_slug)
);

CREATE INDEX idx_company_active ON company (active) WHERE active;

-- ── posting ──────────────────────────────────────────────────────────────
CREATE TABLE posting (
    id                 UUID PRIMARY KEY,
    company_id         UUID NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    ats_posting_id     TEXT NOT NULL,
    title              TEXT NOT NULL,
    location           TEXT,
    remote_type        TEXT NOT NULL DEFAULT 'UNKNOWN'
                          CHECK (remote_type IN ('REMOTE','HYBRID','ONSITE','UNKNOWN','ANY')),
    department         TEXT,
    employment_type    TEXT NOT NULL DEFAULT 'UNKNOWN'
                          CHECK (employment_type IN ('FULL_TIME','INTERNSHIP','CONTRACT','PART_TIME','UNKNOWN')),
    salary_min         NUMERIC,
    salary_max         NUMERIC,
    salary_currency    TEXT,
    url                TEXT,
    description        TEXT,
    sponsorship_signal TEXT NOT NULL DEFAULT 'UNKNOWN'
                          CHECK (sponsorship_signal IN ('OFFERED','NOT_OFFERED','UNKNOWN')),
    posted_at          TIMESTAMPTZ,
    first_seen_at      TIMESTAMPTZ NOT NULL,
    raw                JSONB,
    -- Dedup: a posting is new exactly once per (company, ATS id) — spec §8.3.
    CONSTRAINT uq_posting_natural_key UNIQUE (company_id, ats_posting_id)
);

-- Feed default sort is first_seen_at DESC (spec §5).
CREATE INDEX idx_posting_first_seen ON posting (first_seen_at DESC);
CREATE INDEX idx_posting_company ON posting (company_id);

-- ── app_user ── (table now; adapter + real auth later — D-WD5) ────────────
CREATE TABLE app_user (
    id         UUID PRIMARY KEY,
    username   TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── job_state ── (per-user workflow; adapter in S2, endpoints in S7) ──────
CREATE TABLE job_state (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    posting_id UUID NOT NULL REFERENCES posting (id) ON DELETE CASCADE,
    state      TEXT NOT NULL DEFAULT 'NEW'
                  CHECK (state IN ('NEW','SAVED','APPLIED','HIDDEN')),
    applied_at TIMESTAMPTZ,
    note       TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_state_user_posting UNIQUE (user_id, posting_id)
);

CREATE INDEX idx_job_state_user ON job_state (user_id);

-- ── filter_profile ── (table now; adapter + CRUD in S7) ───────────────────
CREATE TABLE filter_profile (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name              TEXT NOT NULL,
    role_keywords     TEXT[] NOT NULL DEFAULT '{}',
    include_keywords  TEXT[] NOT NULL DEFAULT '{}',
    exclude_keywords  TEXT[] NOT NULL DEFAULT '{}',
    locations         TEXT[] NOT NULL DEFAULT '{}',
    remote_pref       TEXT,
    seniorities       TEXT[] NOT NULL DEFAULT '{}',
    employment_types  TEXT[] NOT NULL DEFAULT '{}',
    salary_min        NUMERIC,
    sponsorship_pref  TEXT,
    posted_within_sec BIGINT,
    seen_from         TIMESTAMPTZ,
    seen_to           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_filter_profile_user ON filter_profile (user_id);
