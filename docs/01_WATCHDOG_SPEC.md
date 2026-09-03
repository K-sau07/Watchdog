# 01 — Watchdog Spec

**Status:** APPROVED v1 (D-WD1..D-WD5 signed off) · **Type:** New product, master spec (immutable base) · **Governed by:** `00_DEVELOPMENT_CONSTITUTION.md`
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
- **posting** — `id, company_id, ats_posting_id (unique per source), title, location, remote(bool/enum), department, employment_type(nullable), salary_min/salary_max/salary_currency(nullable), url, description(text), sponsorship_signal(enum: OFFERED|NOT_OFFERED|UNKNOWN), posted_at(nullable), first_seen_at, raw(jsonb)`. Unique index `(company_id, ats_posting_id)` for dedup. **Description depth:** we store the full ATS description text so keyword include/exclude and the salary/visa heuristics can read the body, not just the title (owner: "every information").
- **job_state** — `id, user_id, posting_id, state(enum: NEW|SAVED|APPLIED|HIDDEN), applied_at(nullable), note(nullable), updated_at`. Unique `(user_id, posting_id)`. Powers the daily workflow: mark applied/saved, dismiss/hide so the radar doesn't re-show handled jobs. Default view hides HIDDEN.
- **filter_profile** — `id, user_id, name, roles(text[]), keywords(text[]), exclude_keywords(text[]), locations(text[]), remote_pref, seniority, employment_types(text[]), salary_min, sponsorship_pref, posted_within, date_from(nullable), date_to(nullable), ...` (drives the dashboard filters).
- **app_user** — minimal auth (reuse TAssist's JWT pattern) so multi-user is possible later.
- (Redis: distributed scheduler lock + poll-cursor/rate-limit state.)

---

*(Part 2 below: matching/filters, the dashboard + cyber-futuristic UI direction, REST API, build order, non-negotiables, open decisions.)*

## 5. Matching & filters (exhaustive — "filter everything out")

Owner wants to filter on *everything*. The dashboard exposes a rich filter panel; every field is also a backend query param so results are server-filtered (fast, scalable).

**Filter dimensions (v1):**
- **Role / title** — keyword + role-family match (e.g. "New Grad", "SWE", "Software Engineer I", "Associate"). Default profile targets early-career SWE.
- **Seniority** — new-grad / intern / junior / mid / senior (inferred from title + keywords).
- **Keywords include / exclude** — free-text must-have and must-not-have (e.g. exclude "Senior", "Staff", "5+ years"). Applies to **title AND description** (see description depth, §4) — so you can exclude "requires 3+ years" or "US citizens only" even when it's buried in the body.
- **Location** — country / region / city; **Remote** (remote-only, hybrid, onsite, any).
- **Dates (explicit — owner emphasized this):**
  - **Posted within** (relative buckets) — 10 min / 30 min / 1 hr / today / this week.
  - **Date range** (absolute) — from/to date pickers on `first_seen_at` (and `posted_at` when the ATS provides it). Filter to an exact day or window.
- **Salary** — min/max range, where the ATS exposes compensation (many Greenhouse/Ashby boards do). Postings without salary can be included/excluded via a toggle.
- **Employment type** — full-time / internship / contract / part-time (from ATS field or inferred).
- **Visa / sponsorship signal** — heuristic flags for "sponsorship available" vs "US citizens/clearance required" parsed from the description. Hugely relevant to the owner's actual hunt. Filter: require-sponsorship / hide-no-sponsorship / any. (Honest: heuristic in v1, refined later.)
- **Company** — filter to/from specific companies; ATS source.
- **Department / team** — where the ATS provides it.
- **Job state (owner's daily workflow)** — filter by NEW / SAVED / APPLIED / HIDDEN (see §4 job states); default view hides HIDDEN and dismisses noise.
- **Freshness sort** — default sort = `first_seen_at DESC` (newest first), always. Also sortable by `posted_at`, salary.

**MatchingService** scores/labels each posting against the active `filter_profile`; the dashboard defaults to the user's profile but every filter is adjustable live in the UI.

**Honest note on title parsing:** seniority/role inference from free-text titles is heuristic (regex + keyword lists) in v1 — good enough, not perfect. If it's weak, a smarter classifier is a Phase 2 lever (measure first, per constitution §10).

## 6. The dashboard — cyber-futuristic, classy, filter-dense (UI direction)

**Vibe (owner):** futuristic "cyber" aesthetic — but *classy*, not gaudy. Think a command-center / radar console for a job hunter. This is where we spend our design boldness (constitution + frontend-design skill: one bold idea, executed with restraint).

**Signature concept:** a **live "radar" feed** — new jobs stream in real-time, freshest pulsing at top, each with a countdown-style "posted 3 min ago" that feels *alive*. The UI should feel like a monitoring console that's actively working for you.

**The signature stat — "how early were you" (the whole thesis, made visible):** every posting shows how fresh it was when Watchdog caught it — e.g. **"caught 4 min after posting."** This is the product's soul on screen: proof you're beating the crowd. A running headline stat ("you're seeing this before ~X% of applicants" / "median catch time today: 6 min") turns the speed advantage into something you can *feel*. This is where we spend design boldness.

**Job card:** title, company, location/remote, **salary (if known), employment type, sponsorship badge**, source badge, **"caught N min after posting"** freshness stamp, apply button, and **quick actions: Save / Applied / Hide** (updates `job_state`, card animates out when hidden). Applied/saved jobs get a subtle state treatment; hidden ones leave the feed.

**Design tokens (dark-first is justified here — a command console, not a study space):**
- Deep space-charcoal / near-black base; a single electric accent (cyber-cyan or electric-violet — pick in a G-UI0 token step, avoid the exact Linear-purple cliché); subtle neon glows used *sparingly* for freshness/state, not everywhere.
- Monospace/technical type for data (timestamps, IDs), a clean sans for content, a sharp display face for headers.
- Grid/HUD motifs, thin rules, tasteful scanline/glow accents — restraint is what makes it "classy" vs "gaudy."
- Motion: fresh-job entries animate in (pulse/glow that fades), reduced-motion-safe. A subtle "live" indicator showing the agent is polling.

**Layout:** left = the exhaustive filter panel (collapsible); center = the live radar feed (job cards, freshest first, freshness glow); a top bar with the "agent status" (last poll, jobs found today, live pulse). Job card: title, company, location/remote, posted-ago, apply button, source badge.

*(Full design bible = a later `02_WATCHDOG_UI_BIBLE.md`, research-grounded like TAssist's glow-up. This section is the direction; the bible comes before UI code.)*

## 7. REST API (dashboard ↔ backend)

- `GET /api/postings?roles=&keywords=&exclude=&location=&remote=&postedWithin=&dateFrom=&dateTo=&salaryMin=&employmentType=&sponsorship=&company=&source=&state=&sort=&page=` → filtered, paginated feed (newest first). Filters span title + description.
- `GET /api/postings/{id}` → full detail (incl. description, salary, sponsorship signal, "caught N min after posting").
- `PUT /api/postings/{id}/state` → set job state (SAVED / APPLIED / HIDDEN) + optional note. Powers the daily workflow.
- `GET /api/agent/status` → last poll time, next poll, companies polled, new-jobs-today count, **median catch-time today** (powers the "live agent" + signature stat).
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
- **S2** — persistence: Flyway (company, posting, job_state, filter_profile, app_user), repos, Testcontainers.
- **S3** — ATS adapters: Greenhouse first (JobSourcePort + GreenhouseClient), normalize → Posting incl. description/salary/employment-type. Unit-test the parser against a captured JSON fixture. Then Lever, then Ashby.
- **S4** — the agent loop: scheduler + Redis lock + PollingService + DedupService; persists new postings + computes catch-time. Measure real poll timing.
- **S5** — company registry: seed a starter set of ATS companies (D-WD1); DiscoveryService.
- **S6** — matching/filters: MatchingService + the filtered `/api/postings` query (all §5 dimensions, title+description) + the salary/seniority/sponsorship parsers (unit-tested pure logic).
- **S7** — **job-state workflow** (save/applied/hide endpoints + repo) + filter-profile CRUD. v1 single-user, no-auth (D-WD5): a fixed user id; schema keeps `user_id` for later. JWT auth deferred to Phase 2.
- **S8** — the dashboard: UI bible first (`02`), then the cyber-futuristic feed + exhaustive filter panel + agent-status bar + "caught N min after posting" signature stat + card state actions.
- **S9** — verify end-to-end: real ATS polling → new job appears on dashboard within the poll window; filters + states work; full gauntlet.
- **Phase 2+** — notifications (email/Telegram), native-Mac menubar notifier, registry auto-expansion, smarter title/visa classifier, multi-user UI.

## 10. Acceptance criteria (v1 done)

- The agent polls real ATS boards on a schedule, resiliently, respecting rate limits.
- A genuinely new posting on a watched company appears on the dashboard within the poll window (measure it).
- Dedup: no posting ever shows twice.
- The dashboard filters work across all §5 dimensions (roles, keywords in title+description, dates/date-range, salary, employment type, sponsorship, location/remote, company, source, job-state), server-side, newest-first.
- **Job states work** — save/applied/hide persist per user; hidden jobs leave the feed; default view excludes hidden.
- **The signature stat is real** — each card shows honest "caught N min after posting"; agent bar shows median catch-time today.
- The "agent status" bar reflects real poll activity.
- Cyber-futuristic UI, reduced-motion-safe, accessible, light on gaud.
- Gauntlet green (local + CI); pure logic unit-tested (parsers, filters, catch-time); end-to-end journey verified.

## 11. Decisions — LOCKED (signed off)

- **D-WD1 ✅** Company discovery = **seed registry now + auto-grow later**. Starter seed = **500 companies** (known Greenhouse/Lever/Ashby boards that hire early-career SWE). Registry auto-expansion is a fast-follow.
- **D-WD2 ✅** Poll interval = **2 minutes**. Note: 500 companies × 2 min is aggressive (~15k calls/hr) — we **stagger/batch across the window** and **back off on 429** (§8.2). Tune empirically (measure-first); if a provider throttles, we adapt. Architecture supports 2 min.
- **D-WD3 ✅** Ship **all three ATS** (Greenhouse, Lever, Ashby) in v1. Built one adapter at a time in build order (Greenhouse → Lever → Ashby), each a green step — "all three" = all in v1, not written simultaneously.
- **D-WD4 ✅** UI = **bold cyber / "robot-era" aesthetic**, classy not gaudy. Exact accent + full system decided in `02_WATCHDOG_UI_BIBLE.md` (research-grounded, options presented). Vibe locked: futuristic command-console radar.
- **D-WD5 ✅** Auth = **deferred**. v1 is **single-user, no-auth** (local tool). Schema stays multi-user-ready (`user_id` columns present); real JWT auth added at multi-user time (Phase 2). This simplifies v1 — no login wall while building the engine.

**Build-order impact of D-WD5:** S7 drops the JWT/login work for now; job-state + filter-profile use a fixed single-user id in v1. Auth slots back in cleanly later because the schema already carries `user_id`.

## 12. Open decisions for sign-off

*(all resolved — see §11)*

---

*End of draft. Per the constitution: no product code until this is signed off and D-WD1..D-WD5 are decided. UI code waits for a `02_WATCHDOG_UI_BIBLE.md`.*
