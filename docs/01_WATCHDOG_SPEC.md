# 01 — Watchdog Spec

**Status:** DRAFT v1 for review · **Type:** New product, master spec (immutable base) · **Governed by:** `00_DEVELOPMENT_CONSTITUTION.md`
**Stack:** Java 21 / Spring Boot (hexagonal) · React 19 / Vite / TS / Tailwind · PostgreSQL · Redis · scheduled poller

> **The thesis:** the early-applicant advantage is real — people get OAs *because they applied minutes after a job posted*. By the time a role hits LinkedIn/Indeed, hundreds have applied. Watchdog is a **continuous agent** that watches where jobs are *born* — company ATS boards — and surfaces fresh matches on a dashboard within minutes of posting, filtered to exactly what you're hunting. Speed is the product.

---

## 1. What v1 is (and isn't)

**v1 core (build this first):** an always-running backend that polls ATS job boards, detects brand-new postings, filters them to the user's criteria (default: New Grad / early-career SWE), and shows them on a **futuristic, filter-rich dashboard**, freshest first, with "posted X min ago" + apply link.

**v1 is NOT:** notifications (email/Telegram/push) or native-Mac alerts. Those are **Phase 2/3** — deliberately deferred. The engine + dashboard must work and prove valuable first. (Owner's explicit call: "notification is the last part.")

**Multi-user:** built for Saurabh first, but the schema + auth are designed so multiple users with their own filters are possible later. No premature multi-tenant complexity in v1 UI.

---

## 2. Why ATS-first (the sourcing decision — the heart of the product)

To be *first*, we read where jobs originate: **Applicant Tracking Systems.** When a recruiter clicks "publish," the role appears on their ATS board instantly — often minutes to hours before it propagates to LinkedIn/Indeed (which are downstream aggregators). We poll the ATS boards directly.

**ATS sources with clean public JSON (no scraping, no anti-bot):**
- **Greenhouse:** `https://boards-api.greenhouse.io/v1/boards/{company}/jobs?content=true`
- **Lever:** `https://api.lever.co/v0/postings/{company}?mode=json`
- **Ashby:** `https://api.ashbyhq.com/posting-api/job-board/{company}`
- (Extensible: Workday, SmartRecruiters, Recruitee later — each an adapter.)

**Why NOT scrape LinkedIn/Indeed:** aggressive anti-bot, ToS violation, brittle, and — critically — *not even faster*, since they're downstream of the ATS. Rejected on engineering grounds.

**D-WD1 (needs sign-off): company discovery.** ATS APIs are per-company, so we need a company registry. Approach: maintain a **growing registry of ATS companies** seeded from public ATS-company directories/lists, expandable over time, rather than the user hand-picking companies. v1: seed a solid starter set (a few hundred known Greenhouse/Lever/Ashby companies that hire new-grad SWE), store them, poll them. Auto-expansion of the registry is a fast-follow. *Rec: seed-registry now, auto-grow next — honest about the mechanism, delivers the "I don't curate companies" experience.*

---

## 3. Architecture (hexagonal, per constitution §7)

```
domain/          Job, Company, AtsSource(enum), Posting, FilterCriteria, SeenPosting;
                 ports: JobSourcePort (out), JobRepository (out), PollingUseCase (in)
application/     PollingService (the agent loop), MatchingService (filters),
                 DiscoveryService (registry), DedupService
infrastructure/  ats/ adapters (GreenhouseClient, LeverClient, AshbyClient),
                 persistence (Postgres + Flyway), scheduling (Spring @Scheduled / Redis),
                 web (REST controllers for the dashboard)
```

**The agent loop (core):**
1. **Scheduler** fires every **N minutes** (configurable, default 5) — Spring `@Scheduled`, distributed-safe via a Redis lock so it never double-runs.
2. For each active company in the registry, call its ATS adapter → normalized `Posting[]`.
3. **Dedup** against `seen_posting` (by stable ATS posting id) → keep only NEW ones.
4. **Timestamp**: capture `firstSeenAt` (our clock) + ATS `postedAt` when available → drives "posted X min ago" and freshness.
5. **Persist** new postings; they flow to the dashboard immediately.
6. **Match** against each user's `FilterCriteria` (v1: Saurabh's) for the dashboard's default view.

**Freshness guarantee:** "minutes after posting" = poll interval + processing. 5-min interval → worst case ~5 min behind the ATS. Tunable down (with rate-limit care).

---

## 4. Data model (Postgres, Flyway)

- **company** — `id, name, ats_source(enum: GREENHOUSE|LEVER|ASHBY), ats_slug, active, last_polled_at, created_at`. (ats_slug = the `{company}` in the API URL.)
- **posting** — `id, company_id, ats_posting_id (unique per source), title, location, remote(bool/enum), department, url, description(text), posted_at(nullable), first_seen_at, raw(jsonb)`. Unique index `(company_id, ats_posting_id)` for dedup.
- **filter_profile** — `id, user_id, name, roles(text[]), keywords(text[]), locations(text[]), remote_pref, seniority, posted_within, ...` (drives the dashboard filters).
- **app_user** — minimal auth (reuse TAssist's JWT pattern) so multi-user is possible later.
- (Redis: distributed scheduler lock + poll-cursor/rate-limit state.)

---

*(Part 2 below: matching/filters, the dashboard + cyber-futuristic UI direction, REST API, build order, non-negotiables, open decisions.)*

## 5. Matching & filters (exhaustive — "filter everything out")

Owner wants to filter on *everything*. The dashboard exposes a rich filter panel; every field is also a backend query param so results are server-filtered (fast, scalable).

**Filter dimensions (v1):**
- **Role / title** — keyword + role-family match (e.g. "New Grad", "SWE", "Software Engineer I", "Associate"). Default profile targets early-career SWE.
- **Seniority** — new-grad / intern / junior / mid / senior (inferred from title + keywords).
- **Keywords include / exclude** — free-text must-have and must-not-have (e.g. exclude "Senior", "Staff", "5+ years").
- **Location** — country / region / city; **Remote** (remote-only, hybrid, onsite, any).
- **Posted within** — 10 min / 30 min / 1 hr / today / this week (drives the "beat the crowd" use case).
- **Company** — filter to/from specific companies; ATS source.
- **Department / team** — where the ATS provides it.
- **Freshness sort** — default sort = `first_seen_at DESC` (newest first), always.

**MatchingService** scores/labels each posting against the active `filter_profile`; the dashboard defaults to the user's profile but every filter is adjustable live in the UI.

**Honest note on title parsing:** seniority/role inference from free-text titles is heuristic (regex + keyword lists) in v1 — good enough, not perfect. If it's weak, a smarter classifier is a Phase 2 lever (measure first, per constitution §10).

## 6. The dashboard — cyber-futuristic, classy, filter-dense (UI direction)

**Vibe (owner):** futuristic "cyber" aesthetic — but *classy*, not gaudy. Think a command-center / radar console for a job hunter. This is where we spend our design boldness (constitution + frontend-design skill: one bold idea, executed with restraint).

**Signature concept:** a **live "radar" feed** — new jobs stream in real-time, freshest pulsing at top, each with a countdown-style "posted 3 min ago" that feels *alive*. The UI should feel like a monitoring console that's actively working for you.

**Design tokens (dark-first is justified here — a command console, not a study space):**
- Deep space-charcoal / near-black base; a single electric accent (cyber-cyan or electric-violet — pick in a G-UI0 token step, avoid the exact Linear-purple cliché); subtle neon glows used *sparingly* for freshness/state, not everywhere.
- Monospace/technical type for data (timestamps, IDs), a clean sans for content, a sharp display face for headers.
- Grid/HUD motifs, thin rules, tasteful scanline/glow accents — restraint is what makes it "classy" vs "gaudy."
- Motion: fresh-job entries animate in (pulse/glow that fades), reduced-motion-safe. A subtle "live" indicator showing the agent is polling.

**Layout:** left = the exhaustive filter panel (collapsible); center = the live radar feed (job cards, freshest first, freshness glow); a top bar with the "agent status" (last poll, jobs found today, live pulse). Job card: title, company, location/remote, posted-ago, apply button, source badge.

*(Full design bible = a later `02_WATCHDOG_UI_BIBLE.md`, research-grounded like TAssist's glow-up. This section is the direction; the bible comes before UI code.)*

## 7. REST API (dashboard ↔ backend)

- `GET /api/postings?roles=&keywords=&exclude=&location=&remote=&postedWithin=&company=&source=&sort=&page=` → filtered, paginated feed (newest first).
- `GET /api/postings/{id}` → full detail.
- `GET /api/agent/status` → last poll time, next poll, companies polled, new-jobs-today count (powers the "live agent" bar).
- `GET/PUT /api/filter-profiles/{id}` → the user's saved filter profile.
- `GET /api/companies` → registry (count, per-source breakdown).
- Auth: JWT (reuse TAssist pattern), minimal for v1 (single user), multi-user-ready.

## 8. Non-negotiables / invariants

1. **No scraping of LinkedIn/Indeed** — ATS JSON APIs only. Legal, clean, actually-fastest.
2. **Respect ATS rate limits** — polite polling, backoff on 429, per-source throttle. Never hammer.
3. **Dedup is exact** — a posting is "new" exactly once, by `(company_id, ats_posting_id)`. No duplicate alerts ever.
4. **Freshness is honest** — show real `first_seen_at`; never fake "posted X ago".
5. **The agent is resilient** — one company/source failing never stops the whole poll cycle; errors logged, cycle continues.
6. **Constitution applies** — spec-first, green gate, measure-before-fix, `main` sacred, no slop.
7. **Notifications are out of scope for v1** — engine + dashboard only.

## 9. Build order (each step green + committed, per constitution §5)

- **S0** — project scaffold (Spring + React + Postgres + Redis + Docker + CI + gauntlet green on empty).
- **S1** — domain layer: models, enums, ports, typed IDs, FilterCriteria. Unit-tested.
- **S2** — persistence: Flyway (company, posting, filter_profile, app_user), repos, Testcontainers.
- **S3** — ATS adapters: Greenhouse first (JobSourcePort + GreenhouseClient), normalize → Posting. Unit-test the parser against a captured JSON fixture. Then Lever, then Ashby.
- **S4** — the agent loop: scheduler + Redis lock + PollingService + DedupService; persists new postings. Measure real poll timing.
- **S5** — company registry: seed a starter set of ATS companies (D-WD1); DiscoveryService.
- **S6** — matching/filters: MatchingService + the filtered `/api/postings` query.
- **S7** — auth (JWT, minimal) + filter-profile CRUD.
- **S8** — the dashboard: UI bible first (`02`), then the cyber-futuristic feed + filter panel + agent-status bar.
- **S9** — verify end-to-end: real ATS polling → new job appears on dashboard within the poll window; full gauntlet.
- **Phase 2+** — notifications (email/Telegram), native-Mac menubar notifier, registry auto-expansion, smarter title classifier, multi-user UI.

## 10. Acceptance criteria (v1 done)

- The agent polls real ATS boards on a schedule, resiliently, respecting rate limits.
- A genuinely new posting on a watched company appears on the dashboard within the poll window (measure it).
- Dedup: no posting ever shows twice.
- The dashboard filters work across all §5 dimensions, server-side, newest-first.
- The "agent status" bar reflects real poll activity.
- Cyber-futuristic UI, reduced-motion-safe, accessible, light on gaud.
- Gauntlet green (local + CI); pure logic unit-tested; end-to-end journey verified.

## 11. Open decisions for sign-off

- **D-WD1:** company discovery — seed registry now + auto-grow later (rec: yes). How big a starter seed? (100? 300? 500 companies?)
- **D-WD2:** poll interval for v1 — 5 min a good default? (Lower = fresher but more API load.)
- **D-WD3:** which ATS first — Greenhouse (biggest new-grad SWE presence)? (rec: Greenhouse → Lever → Ashby.)
- **D-WD4:** accent color for the cyber aesthetic — cyber-cyan, electric-violet, or decide during the UI bible? (rec: decide in UI bible with options.)
- **D-WD5:** auth in v1 — full JWT now, or a single-user no-auth local tool first and add auth at multi-user time? (rec: minimal JWT now since schema's multi-user-ready.)

---

*End of draft. Per the constitution: no product code until this is signed off and D-WD1..D-WD5 are decided. UI code waits for a `02_WATCHDOG_UI_BIBLE.md`.*
