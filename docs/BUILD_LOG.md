# Watchdog — BUILD_LOG

Living state document. **Read this + the spec at the start of every session.** Append a dated entry at every meaningful milestone (files shipped, verification result, decisions). Governed by `00_DEVELOPMENT_CONSTITUTION.md`.

---

## Project snapshot

- **What:** continuous job-hunting agent — polls ATS boards (Greenhouse/Lever/Ashby) every 2 min, detects brand-new postings, filters to my criteria (New Grad / early-career SWE), surfaces on a cyber-futuristic radar dashboard, freshest first. Speed is the product.
- **Repo:** `github.com/K-sau07/Watchdog` · local `/Users/saurabhkashyap/Desktop/Watchdog/`
- **Stack:** Java 21 / Spring Boot (hexagonal) · React 19 / Vite / TS / Tailwind · Postgres + Flyway · Redis · Testcontainers · Docker Compose · GitHub Actions.
- **Spec:** `docs/01_WATCHDOG_SPEC.md` (APPROVED, immutable base). **Process:** `docs/00_DEVELOPMENT_CONSTITUTION.md`.

## Locked decisions (D-WD1..D-WD8)
- **D-WD1** — seed registry, **500 companies**; auto-grow later.
- **D-WD2** — **2-min** poll; staggered across the window + backoff on 429; tune empirically.
- **D-WD3** — all three ATS in v1 (Greenhouse → Lever → Ashby, built one at a time).
- **D-WD4** — bold cyber "robot-era" UI; exact system in `02_WATCHDOG_UI_BIBLE.md` before any UI code.
- **D-WD5** — auth deferred; v1 single-user no-auth; schema stays multi-user-ready (`user_id` columns present).
- **D-WD6** — **Spring Boot 4.1.0** on Java 21 (spec-implementation). Chosen over 3.5.x, which hit OSS end-of-life 2025-06-30 (starting a new project on an already-EOL framework = born on borrowed time). 4.1 is the current stable / official new-project target. Cost: 4.x is modular (see gotchas) so some 3.x tutorials don't apply.
- **D-WD7** — persistence via **Spring Data JDBC** (infra-implementation), over JPA/Hibernate and plain JdbcClient. Fits immutable domain records + self-contained aggregates with no ORM baggage; least code for the cleanest result. Domain stays pure — annotated row entities + adapters live in `infrastructure/persistence`, mapping row↔domain.
- **D-WD8** — agent-loop distributed lock via **ShedLock 6.9.0 + Spring `@Scheduled`**, Redis-backed (infra-implementation), over a hand-rolled `SET NX` lock or no lock. Purpose-built for "run scheduled task on one instance"; hand-rolling correct lock expiry/renewal is exactly the subtle-breakage the constitution warns against.

## Build order & status
- [x] **S0** — project scaffold (Spring + React + Postgres + Redis + Docker + CI; gauntlet green on empty) ✅
- [x] **S1** — domain layer (models, enums, ports, typed IDs, FilterCriteria) + unit tests ✅
- [x] **S2** — persistence (Flyway: company, posting, job_state, filter_profile, app_user) + Testcontainers ✅
- [x] **S3** — ATS adapters (Greenhouse → Lever → Ashby), normalize → Posting (incl. description/salary/type) + fixture parser tests ✅
- [x] **S4** — agent loop (scheduler + Redis lock + PollingService + DedupService + catch-time); measure poll timing ✅
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

### 2026 — Session 3: S2 persistence (COMPLETE)
Branch `s2-persistence`, merged to `main --no-ff`. Spring Data JDBC (D-WD7) over real Postgres 17 via Testcontainers. Full gauntlet green: **50 tests** (35 domain/scaffold + 15 persistence integration), jar packaged. Domain still pure — all persistence annotations confined to `infrastructure/persistence`.
- **S2.1** `8bd0cb8` — swapped POM starter-data-jpa → starter-data-jdbc. `V1__initial_schema.sql`: all 5 tables (§4), enums as text+CHECK (Java constant names), UUID PKs (domain-minted), timestamptz, dedup unique `(company_id, ats_posting_id)` §8.3, job_state unique `(user_id, posting_id)`, raw jsonb, filter_profile text[]. Testcontainers harness `PostgresIntegrationTest` — **singleton container pattern** (start once, never stop; the `@Testcontainers`/`@Container` lifecycle stops it after the first class, breaking shared bases — diagnosed from a Connection-refused on the 2nd class). `@ServiceConnection` auto-wires datasource; test profile no longer excludes datasource. 4 tests.
- **S2.2** `763919a` — company adapter. `CompanyRow` implements `Persistable<UUID>` (domain-minted UUIDs are never null, so isNew comes from an `existsById` check, not id==null). Derived finders + `@Query` findAllActive. 5 tests.
- **S2.3** `2723ca2` — posting adapter (jsonb + salary + dedup). `JsonbString` wrapper type so jsonb converters target ONLY jsonb columns (raw String↔jsonb would coerce every String column); `JdbcConfig.userConverters()` registers JsonbString↔PGobject. Salary flattened to 3 cols. **POM fix:** postgres driver runtime→compile (converter references PGobject; caught at compile). 4 tests.
- **S2.4** `726b4ca` — job-state adapter. Natural key `(user_id, posting_id)`. Test inserts a fixture app_user via JdbcClient (app_user adapter deferred to S7). 3 tests.
- **S2.5** — full gauntlet green (50 tests), BUILD_LOG + D-WD7, merged to `main`.
- **Decision:** **D-WD7** = Spring Data JDBC.
- **Deferred intentionally:** app_user + filter_profile tables exist (V1) but their repo adapters wait for S7 (no adapters built we don't use).
- **Next:** S3 — ATS adapters (Greenhouse first). Real HTTP against boards-api.greenhouse.io; parse → Posting; unit-test the parser against a captured JSON fixture.

### 2026 — Session 4: S3 ATS adapters (COMPLETE)
Branch `s3-greenhouse`, merged to `main --no-ff`. All three `JobSourcePort` implementations (Greenhouse → Lever → Ashby), each **measured against a real captured fixture** (not guessed), parser + RestClient, MockWebServer HTTP tests. Full gauntlet green: **82 tests**, jar packaged. Domain still pure.
- **S3.1** `8c70ae1` — Greenhouse (parser + fixture). Fields: `id`→atsPostingId, `title`, `location.name`, `content`→description (HTML entities decoded via `HtmlUtils.htmlUnescape`), `first_published`→postedAt (fallback `updated_at`), `departments[0].name`. No structured salary/type → UNKNOWN. 8 tests.
- **S3.2** `1a43c4d` — Greenhouse `RestClient` client (`JobSourcePort`), MockWebServer test. **Bug caught only by `clean verify`:** component depended on an injected `RestClient.Builder` bean absent in the test context (16 context-load errors) → fix: build `RestClient.builder()` inside the component, no bean dependency. Added `TimeConfig` (single injectable `Clock` bean, honest catch-time). 3 tests.
- **S3.3** `fc253a3` — Lever (parser + client). Different shape: `text`→title, `categories.location`/`team`, `workplaceType`→RemoteType (real remote signal!), `createdAt` **epoch millis**→postedAt, `descriptionPlain`. Rejects non-array (misses return `{"ok":false}`). `GET /v0/postings/{slug}?mode=json`. Fixture = `leverdemo` (stable official demo). 10 tests.
- **S3.4** `cf611fd` — Ashby (parser + client). Richest source: `jobs[]` under a wrapper obj, `employmentType` (FullTime/Intern/Contract/PartTime)→**structured EmploymentType** (only Ashby has it), `workplaceType`+`isRemote` fallback→RemoteType, `publishedAt`→postedAt. `GET /posting-api/job-board/{slug}`. Fixture = `linear`. 11 tests.
- **S3.5** — full gauntlet green (82 tests), BUILD_LOG, merged to `main`.
- **Impl decisions (no new D#):** HTTP via Spring `RestClient`; HTTP tests via OkHttp **MockWebServer** (real localhost HTTP, per Spring's own docs — catches client I/O differences a stub misses).
- **Deferred:** salary + seniority + sponsorship parsing from titles/bodies → S6 (pure heuristics, own tests). Adapters are not yet wired into a poll loop → S4. No `JobSourcePort` router/registry yet → S4.
- **Next:** S4 — the agent loop (scheduler + Redis lock + PollingService + DedupService + catch-time). First step that runs continuously.

### 2026 — Session 5: S4 agent loop (COMPLETE)
Branch `s4-agent-loop`, merged to `main --no-ff`. **The engine is alive** — verified LIVE against the real stack, not just tests: booted the app, scheduler fired every 20s, seeded Linear (Ashby) → one cycle polled 1 company → **29 real postings persisted**, next cycle found **0 new (dedup works in production)**. Full gauntlet green: **92 tests**, jar packaged. Domain still pure.
- **S4.1+S4.2** `643d33c` — `AtsSourceRouter` (injects all `JobSourcePort` beans, indexes by `source()`, self-wires new adapters, rejects dup/missing) + `PollingService implements PollingUseCase.runOnce()`: per active company route→fetch→dedup by natural key→persist new→mark `polledAt`; resilient (one source failure caught/logged/counted, cycle continues §8.5); median catch-time over new postings w/ known postedAt (empty when none §8.4). 10 unit tests via in-memory fakes (no Spring/DB/HTTP).
- **S4.3+S4.4** `b1e5bf4` — Redis lock + scheduler (D-WD8). Deps: `spring-boot-starter-data-redis` + `shedlock-spring` + `shedlock-provider-redis-spring` 6.9.0. `SchedulingConfig` (`@EnableScheduling` + `@EnableSchedulerLock`, `RedisLockProvider` env "watchdog"). `PollScheduler` `@Scheduled(fixedDelay = watchdog.polling.interval-ms default 120000 = D-WD2)` + `@SchedulerLock(lockAtMostFor PT5M, lockAtLeastFor PT5S)`. Both gated by `watchdog.polling.enabled` (default true; **false in test profile** → scheduler off in tests, no Redis needed). Cleaned dead JPA config from dev profile (we're on JDBC), added Redis conn.
- **S4.5** — full gauntlet green (92 tests), BUILD_LOG + D-WD8, merged to `main`.
- **Decision:** **D-WD8** = ShedLock + `@Scheduled`.
- **Live-run note:** app default port 8080 collided (something else on the Mac) — ran the manual verification on `--server.port=8090`. The 8080 occupant is unrelated to Watchdog.
- **Deferred:** rate-limit/backoff + staggering across the window (D-WD2 detail) → tune in S5/S9 with the 500-company seed (measure-first). Salary/seniority/sponsorship parsing → S6. Registry is still hand-seeded (one company) → S5 DiscoveryService + 500-seed.
- **Next:** S5 — company registry: seed 500 known Greenhouse/Lever/Ashby companies (D-WD1) + DiscoveryService.

---

## Environment gotchas (append as discovered)
- Mac file/shell ops via Desktop Commander (bash sandbox can't see real FS). `edit_block` for edits; `write_file` needs `content`. **DC MCP loads as deferred tools — run `tool_search` first** (e.g. "desktop commander filesystem read write"); don't assume it's absent.
- `export JAVA_HOME=.../temurin-21.jdk/.../Home` before mvn. Frontend: `export PATH="$HOME/.nvm/versions/node/v20.20.1/bin:$PATH"` + `unset NODE_ENV` before npm.
- Source `.env` before backend; never commit secrets.
- Long processes: `nohup` + poll the log. No `timeout` on macOS; kill by port (`lsof -ti:PORT | xargs kill -9`). **`docker compose up` can hang the MCP call even as it succeeds — fire with `nohup` + poll, and check `docker ps` state before re-running.**
- Background `git push` can lag/lose cwd — verify `git ls-remote` vs local HEAD; retry in foreground. **Confirmed fix: push synchronously with explicit redirect — `git push origin BR:BR > /tmp/push.log 2>&1; echo exit=$?` — the backgrounded `git push -u &` form silently fails to land the ref.**
- **Spring Boot 4.x is modular (breaking vs 3.x):** `spring-boot-starter-web` → `spring-boot-starter-webmvc`; `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure` and needs `spring-boot-starter-webmvc-test` (not transitive); `@MockBean` removed → use `@MockitoBean`; Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql` (raw `flyway-core` silently no-ops).
- **Boot 4 ships Jackson 3:** packages are `tools.jackson.databind.*` / `tools.jackson.core.*` (NOT `com.fasterxml.jackson.*`). `ObjectMapper.readTree` is now unchecked. Old Jackson-2 imports won't compile.
- **RestClient bean gotcha:** an injected `RestClient.Builder` bean isn't always present in test contexts. Build `RestClient.builder()...build()` inside the component instead of depending on the bean. Test RestClient with OkHttp MockWebServer (real localhost HTTP). Run `mvn clean verify` before commit — isolated `-Dtest=` runs can hide context-load failures.
- **Testcontainers 2.x renamed modules:** `testcontainers-junit-jupiter` / `testcontainers-postgresql` (old `junit-jupiter` / `postgresql` artifact IDs gone). BOM import needs explicit version in dependencyManagement.
- **Frontend toolchain (current create-vite):** ships Vite 8 / React 19.2 / TS 6, and **oxlint** as the default linter (not ESLint). Tailwind v4 is CSS-first: no `tailwind.config.js`, no PostCSS config.
