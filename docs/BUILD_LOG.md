# Watchdog — BUILD_LOG

Living state document. **Read this + the spec at the start of every session.** Append a dated entry at every meaningful milestone (files shipped, verification result, decisions). Governed by `00_DEVELOPMENT_CONSTITUTION.md`.

---

## Project snapshot

- **What:** continuous job-hunting agent — polls ATS boards (Greenhouse/Lever/Ashby) every 2 min, detects brand-new postings, filters to my criteria (New Grad / early-career SWE), surfaces on a cyber-futuristic radar dashboard, freshest first. Speed is the product.
- **Repo:** `github.com/K-sau07/Watchdog` · local `/Users/saurabhkashyap/Desktop/Watchdog/`
- **Stack:** Java 21 / Spring Boot (hexagonal) · React 19 / Vite / TS / Tailwind · Postgres + Flyway · Redis · Testcontainers · Docker Compose · GitHub Actions.
- **Spec:** `docs/01_WATCHDOG_SPEC.md` (APPROVED, immutable base). **Process:** `docs/00_DEVELOPMENT_CONSTITUTION.md`.

## Locked decisions (D-WD1..D-WD6)
- **D-WD1** — seed registry, **500 companies**; auto-grow later.
- **D-WD2** — **2-min** poll; staggered across the window + backoff on 429; tune empirically.
- **D-WD3** — all three ATS in v1 (Greenhouse → Lever → Ashby, built one at a time).
- **D-WD4** — bold cyber "robot-era" UI; exact system in `02_WATCHDOG_UI_BIBLE.md` before any UI code.
- **D-WD5** — auth deferred; v1 single-user no-auth; schema stays multi-user-ready (`user_id` columns present).
- **D-WD6** — **Spring Boot 4.1.0** on Java 21 (spec-implementation). Chosen over 3.5.x, which hit OSS end-of-life 2025-06-30 (starting a new project on an already-EOL framework = born on borrowed time). 4.1 is the current stable / official new-project target. Cost: 4.x is modular (see gotchas) so some 3.x tutorials don't apply.

## Build order & status
- [x] **S0** — project scaffold (Spring + React + Postgres + Redis + Docker + CI; gauntlet green on empty) ✅
- [x] **S1** — domain layer (models, enums, ports, typed IDs, FilterCriteria) + unit tests ✅
- [ ] **S2** — persistence (Flyway: company, posting, job_state, filter_profile, app_user) + Testcontainers
- [ ] **S3** — ATS adapters (Greenhouse → Lever → Ashby), normalize → Posting (incl. description/salary/type) + fixture parser tests
- [ ] **S4** — agent loop (scheduler + Redis lock + PollingService + DedupService + catch-time); measure poll timing
- [ ] **S5** — company registry (500-company seed) + DiscoveryService
- [ ] **S6** — matching/filters (all §5 dims, title+description) + salary/seniority/sponsorship parsers (pure logic, tested)
- [ ] **S7** — job-state workflow (save/applied/hide) + filter-profile CRUD (single-user, no-auth)
- [ ] **S8** — dashboard: UI bible (`02`) first, then cyber feed + filter panel + agent-status bar + "caught N min after posting" stat + card state actions
- [ ] **S9** — end-to-end verify (real ATS → dashboard within poll window; filters + states) + full gauntlet
- **Phase 2+** — notifications (email/Telegram), native-Mac menubar notifier, registry auto-expansion, smarter title/visa classifier, multi-user auth + UI

## Session log

### 2025 — Session 0: bootstrap + spec (COMPLETE)
- Bootstrapped repo, copied in the dev constitution.
- Wrote + reviewed + finalized `01_WATCHDOG_SPEC.md`; full audit against requirements; folded in 4 gaps (date-range/salary/employment-type/visa filters, keyword-in-description, job-state workflow, description depth, "caught N min after posting" signature stat). Status APPROVED.
- Locked D-WD1..D-WD5.
- Commits: `6a4710b` (bootstrap), `ab4eac3` (finalized spec). Pushed to `origin/main`.
- **Next:** S0 — the scaffold.

### 2026 — Session 1: S0 scaffold (COMPLETE)
Branch `s0-scaffold`, granular green commits, merged to `main --no-ff`. Full gauntlet green locally **and** on GitHub Actions.
- **S0.1** `269c349` — backend skeleton: Spring Boot 4.1.0 / Java 21, hexagonal (`domain`/`application`/`infrastructure`, empty pkgs via `.gitkeep`), `GET /api/health` → `{status:"UP"}` (typed record). Flyway+JPA wired dormant, Testcontainers in for S2. `test` profile excludes datasource so context loads DB-free. 2 tests green.
- **S0.2** `6d431f0` — frontend shell: create-vite react-ts → Vite 8 / React 19.2 / TS 6, linter is **oxlint** (Vite's new default, not ESLint), Tailwind v4 CSS-first (`@tailwindcss/vite` + `@import "tailwindcss"`, no config file), Vitest smoke test. Demo boilerplate stripped to a "Watchdog" placeholder — **no cyber UI yet** (waits for doc 02 per D-WD4). 4 gates green.
- **S0.3** `e80a447` — infra: `docker-compose.yml` Postgres 17-alpine + Redis 7-alpine, healthchecks, gitignored volumes; `.env.example` (placeholders only). Verified live: psql → PostgreSQL 17.11, redis-cli → PONG.
- **S0.4** `962fde1` — CI: `.github/workflows/ci.yml` on push/PR, backend (`mvn -B verify`, DB-free) + frontend (`npm ci` → typecheck → lint → test → build). Actions pinned checkout@v5 / setup-java@v6 / setup-node@v6. **First run green on both jobs.**
- **S0.5** — final gauntlet green (BE 2 tests; FE typecheck/lint/test/build), BUILD_LOG updated, merged to `main`.
- Decision locked: **D-WD6** (Spring Boot 4.1.0 over EOL 3.5.x).
- **Infra note:** host ports 5432/6379 collide with the separate `tassist` stack; tassist was **stopped** to free them (restart with `docker compose up -d` in that project when needed).
- **Next:** S1 — domain layer.

### 2026 — Session 2: S1 domain layer (COMPLETE)
Branch `s1-domain`, granular green commits, merged to `main --no-ff`. Pure domain — **zero framework imports** (verified mechanically: only `java.*` + `com.watchdog.domain.*`). 35 tests green (`mvn clean verify`), jar packaged.
- **S1.1** `bba00d9` — enums (`AtsSource`, `JobState`, `SponsorshipSignal`, `EmploymentType`, `Seniority`, `RemoteType` with `ANY`=no-preference) + typed IDs (`CompanyId`/`PostingId`/`JobStateId`/`FilterProfileId`/`UserId` — UUID-wrapping records, `generate()` domain-minted, value equality, null-rejecting). 6 tests.
- **S1.2** `e8df233` — models: `Salary` (all-optional value obj, `empty()` not null), `Company` (`register()` + immutable `polledAt()`/`withActive()`), `Posting` (nullable ATS fields normalized to UNKNOWN/empty; **`catchTime()`/`catchMinutes()`** = signature stat, honest `Optional` — empty when no `postedAt`, clamped to 0 on clock skew, never faked per §8.4; `rawJson` kept), `JobStateRecord` (`transitionTo()` stamps `appliedAt` on APPLIED, preserves on re-apply). 19 tests.
- **S1.3** `cf56b16` — `FilterCriteria` (builder value obj over every §5 dim; `all()`=match-all; clock-free time filters) + `PostingMatcher` (pure predicate, caller passes `now`). Role kw→title only; include kw→title+description AND; exclude→neither; location/remote/employment/salary-floor(top-of-band + include-unknown toggle)/sponsorship/postedWithin/absolute-window. Deliberately excludes seniorities (needs S6 parser), sources (Company attr), statesToShow (per-user) → resolved at S6/S7 query layer, no silently-ignored constraints. 33 tests.
- **S1.4** `79bcfba` — ports: out — `JobSourcePort` (per-ATS fetch, `source()` routing), `PostingRepository` (dedup by natural key `(companyId, atsPostingId)` §8.3), `CompanyRepository` (`findActive()` work list), `JobStateRepository`; in — `PollingUseCase.runOnce()` + `PollCycleResult` (polled/failed/new + `Optional` median catch-time). Split persistence into per-aggregate repos vs spec's single `JobRepository` (focused contracts; naming refinement only).
- **S1.5** — full gauntlet green (35 tests), BUILD_LOG updated, merged to `main`.
- **Decisions (spec-impl, no new D#):** typed IDs wrap **UUID** (domain-generated); matching = **pure predicate in domain**, S6 orchestrates.
- **Next:** S2 — persistence (Flyway + repos + Testcontainers). Note: this is where the Watchdog Postgres/Redis stack + Testcontainers come alive; tassist still stopped.

---

## Environment gotchas (append as discovered)
- Mac file/shell ops via Desktop Commander (bash sandbox can't see real FS). `edit_block` for edits; `write_file` needs `content`. **DC MCP loads as deferred tools — run `tool_search` first** (e.g. "desktop commander filesystem read write"); don't assume it's absent.
- `export JAVA_HOME=.../temurin-21.jdk/.../Home` before mvn. Frontend: `export PATH="$HOME/.nvm/versions/node/v20.20.1/bin:$PATH"` + `unset NODE_ENV` before npm.
- Source `.env` before backend; never commit secrets.
- Long processes: `nohup` + poll the log. No `timeout` on macOS; kill by port (`lsof -ti:PORT | xargs kill -9`). **`docker compose up` can hang the MCP call even as it succeeds — fire with `nohup` + poll, and check `docker ps` state before re-running.**
- Background `git push` can lag/lose cwd — verify `git ls-remote` vs local HEAD; retry in foreground. **Confirmed fix: push synchronously with explicit redirect — `git push origin BR:BR > /tmp/push.log 2>&1; echo exit=$?` — the backgrounded `git push -u &` form silently fails to land the ref.**
- **Spring Boot 4.x is modular (breaking vs 3.x):** `spring-boot-starter-web` → `spring-boot-starter-webmvc`; `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure` and needs `spring-boot-starter-webmvc-test` (not transitive); `@MockBean` removed → use `@MockitoBean`; Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql` (raw `flyway-core` silently no-ops).
- **Testcontainers 2.x renamed modules:** `testcontainers-junit-jupiter` / `testcontainers-postgresql` (old `junit-jupiter` / `postgresql` artifact IDs gone). BOM import needs explicit version in dependencyManagement.
- **Frontend toolchain (current create-vite):** ships Vite 8 / React 19.2 / TS 6, and **oxlint** as the default linter (not ESLint). Tailwind v4 is CSS-first: no `tailwind.config.js`, no PostCSS config.
