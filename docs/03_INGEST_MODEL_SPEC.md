# 03 — Ingest Model Rework (D-WD23)

**Status:** APPROVED (owner: "fix our whole request model") · **Type:** Architecture change to the poll/persist path · **Governed by:** `00_DEVELOPMENT_CONSTITUTION.md`, extends `01_WATCHDOG_SPEC.md`

> **The shift:** stop hoarding every job and filtering on read. Only **persist what's in the user's universe** (software roles, US). Pull less to process, store less, and every downstream problem (storage bloat, company flooding, retention pressure, the old 2000-cap) shrinks with it.

---

## Why (the problem this fixes)

Original model: poll each board → persist **every** posting → filter on read. At 134 boards that meant ~13k rows, most of them roles the owner will never hunt (sales, marketing, recruiting, non-SWE eng). Consequences we hit in practice: DB bloat, one big company flooding the feed, aggressive retention needed, and read-time filtering over a huge candidate set.

**Root cause:** we treated the DB as a universal archive instead of *the user's hunt*. D-WD23 flips that.

## Hard constraint (why this is ingest-side, not request-side)

Greenhouse / Lever / Ashby APIs have **no server-side search** — one URL returns the whole board, no title/date/location query params. So for these three we cannot fetch less; we can only **keep less**. The filter therefore lives at **ingest** (before persist), not in the request. (Workday *does* support `searchText` + pagination — that request-side filtering is spec'd separately in `04`.)

---

## D-WD23 — Ingest-time filtering

**Rule:** a fetched posting is persisted only if it matches the **ingest profile** (a coarse superset), evaluated in `PollingService` before `save`. Non-matching postings are dropped, never stored.

**Two-tier filtering (important):**
- **Ingest filter (coarse, this doc):** "is this even in my universe" — software role + US. Defines what's *stored*.
- **Read filter (fine, existing `MatchingService`/`FilterCriteria`):** "what do I want right now" — seniority, salary, posted-within, sponsorship, source, company. Narrows *within* the stored set.

The UI filters keep working exactly as today — they just operate over a leaner, pre-qualified table.

### D-WD23b — the coarse net: `SoftwareRoleMatcher` (pure domain)

Keep SWE / data / ML / infra-engineering roles; drop sales/devrel/support/non-software. Comprehensive **software vocabulary + targeted exclusions**, tuned against a live audit of the posting table (kept ~4,028 of ~5k; leaks were real software sub-teams, misses were correctly-dropped devrel/sales).

- **INCLUDE** (any match, case-insensitive, on TITLE): software engineer/developer, swe, sde, backend, frontend, full-stack, web/mobile/iOS/Android engineer, devops, sre, site reliability, platform/infrastructure/systems engineer, data engineer, machine learning, ml/ai engineer, applied ai/ml, deep learning, data scientist, research engineer/scientist, security engineer, appsec, cloud engineer, embedded, firmware, distributed systems, member of technical staff / mts, software architect, principal/staff engineer, programmer, forward deployed engineer / fde, "engineer I/II/III/1-4", "software engineer N", "software development", "agent developer".
- **EXCLUDE** (drop even if INCLUDE hit): solutions architect/engineer/consultant, sales engineer, pre-sales, gtm engineer (bare), customer engineer/success, technical services/support, field engineer, design/hardware/mechanical/electrical/civil engineer, systems administrator, network engineer, qa/test/quality engineer, account executive, business/marketing/recruiting, developer advocate/educator/engagement (devrel), data annotation.
- **US filter:** reuse the existing `UsLocationClassifier` (D-WD17) at ingest too — drop clearly-foreign locations. (Ambiguous/remote kept, same inclusive rule.)

Pure, framework-free, unit-tested against the audited title set.

### Consequences (owner-approved tradeoffs)

1. **The DB reflects the current hunt, not everything.** A role type outside the ingest net is never stored; changing the net only collects such roles *going forward* (no backfill). Acceptable — the ingest net is deliberately broad (all software), so the UI can still slice freely within it.
2. **`usOnly` at read-time becomes mostly redundant** (already US-filtered at ingest) but stays for correctness on any ambiguous rows kept.
3. **Existing rows:** a one-time cleanup drops already-stored non-software rows so the live DB matches the new rule immediately (like the retention prune).

---

## Build order (green commits, `s11-ingest-filter`)

- **S11.1** `SoftwareRoleMatcher` (pure domain) + unit tests (audited include/exclude vocab).
- **S11.2** Wire ingest filter into `PollingService.pollCompany`: persist only if `SoftwareRoleMatcher.isSoftwareRole(title)` AND `UsLocationClassifier.isUnitedStates(location)`. Count skipped. Unit-tested with fakes.
- **S11.3** One-time cleanup of existing non-software rows (SQL, protect job_state like retention).
- **S11.4** Verify live: poll → DB only grows with software/US rows; confirm counts. BUILD_LOG + D-WD23.

Workday adapter (searchText + stop-paginating + N-days-ago dates + per-tenant registry) = separate spec `04`, next.
