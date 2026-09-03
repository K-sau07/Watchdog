# Watchdog — BUILD_LOG

Living state document. **Read this + the spec at the start of every session.** Append a dated entry at every meaningful milestone (files shipped, verification result, decisions). Governed by `00_DEVELOPMENT_CONSTITUTION.md`.

---

## Project snapshot

- **What:** continuous job-hunting agent — polls ATS boards (Greenhouse/Lever/Ashby) every 2 min, detects brand-new postings, filters to my criteria (New Grad / early-career SWE), surfaces on a cyber-futuristic radar dashboard, freshest first. Speed is the product.
- **Repo:** `github.com/K-sau07/Watchdog` · local `/Users/saurabhkashyap/Desktop/Watchdog/`
- **Stack:** Java 21 / Spring Boot (hexagonal) · React 19 / Vite / TS / Tailwind · Postgres + Flyway · Redis · Testcontainers · Docker Compose · GitHub Actions.
- **Spec:** `docs/01_WATCHDOG_SPEC.md` (APPROVED, immutable base). **Process:** `docs/00_DEVELOPMENT_CONSTITUTION.md`.

## Locked decisions (D-WD1..D-WD5)
- **D-WD1** — seed registry, **500 companies**; auto-grow later.
- **D-WD2** — **2-min** poll; staggered across the window + backoff on 429; tune empirically.
- **D-WD3** — all three ATS in v1 (Greenhouse → Lever → Ashby, built one at a time).
- **D-WD4** — bold cyber "robot-era" UI; exact system in `02_WATCHDOG_UI_BIBLE.md` before any UI code.
- **D-WD5** — auth deferred; v1 single-user no-auth; schema stays multi-user-ready (`user_id` columns present).

## Build order & status
- [ ] **S0** — project scaffold (Spring + React + Postgres + Redis + Docker + CI; gauntlet green on empty)
- [ ] **S1** — domain layer (models, enums, ports, typed IDs, FilterCriteria) + unit tests
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

---

## Environment gotchas (append as discovered)
- Mac file/shell ops via Desktop Commander (bash sandbox can't see real FS). `edit_block` for edits; `write_file` needs `content`.
- `export JAVA_HOME=.../temurin-21.jdk/.../Home` before mvn. Frontend: `export PATH="$HOME/.nvm/versions/node/v20.20.1/bin:$PATH"` + `unset NODE_ENV` before npm.
- Source `.env` before backend; never commit secrets.
- Long processes: `nohup` + poll the log. No `timeout` on macOS; kill by port (`lsof -ti:PORT | xargs kill -9`).
- Background `git push` can lag/lose cwd — verify `git ls-remote` vs local HEAD; retry in foreground.
