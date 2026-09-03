# 00 — Development Constitution

**A department-grade operating system for solo builds (Saurabh + Claude).**
Stack focus: **Java 21 / Spring Boot + React 19 / TypeScript.** Reuse this in every new project — drop it in as `docs/00_DEVELOPMENT_CONSTITUTION.md` before writing a single line of product code.

> This is not project documentation. It is the *method* — the rules for HOW we build, distilled from TAssist. The point: one engineer + one AI produce output with the discipline of a full SE department — clean specs, deliberate decisions, green gates, no AI slop, no broken `main`, ever.

---

## 0. The Prime Directives (non-negotiable)

1. **Spec before code. Always.** No product code exists before a written, committed spec describes what we're building and why. If code starts before a spec, STOP and write the spec — even retroactively — then reconcile.
2. **`main` is sacred.** `main` always builds, always passes the gauntlet, always deployable. A broken commit never touches `main`.
3. **Green gate before every commit.** typecheck → lint → test → build. All green. No exceptions, no "I'll fix it next commit."
4. **Small, reversible steps.** Granular commits at every green milestone. Never a giant commit; never a commit that mixes five concerns.
5. **Measure before fixing.** No speculative patching. Diagnose with evidence (logs, tests, real data) before changing behavior.
6. **Decisions are written, not silent.** Every real fork is a numbered decision with tradeoffs, recorded before building the chosen path.
7. **No AI slop.** Every line is understood, intentional, and consistent with the existing codebase's patterns. If Claude generates something that doesn't fit the architecture or that neither of us can explain, it doesn't ship.

---

## 1. Project bootstrap (day zero, before any product code)

Create the docs skeleton first:
```
docs/
  00_DEVELOPMENT_CONSTITUTION.md   ← this file (copied in)
  01_<PROJECT>_SPEC.md             ← the master spec (immutable base)
  BUILD_LOG.md                     ← living state / session continuity
  ADR/ (optional)                  ← architecture decision records if many
```
Then, in order:
1. **Write `01_<PROJECT>_SPEC.md`** — the master spec (§3). Commit it *before* scaffolding.
2. **Scaffold the repo** — Spring Boot + React skeletons, hexagonal layout, tooling (see §7). One commit: "chore: project scaffold."
3. **Set up the gauntlet** — make sure typecheck/lint/test/build all run and pass on the empty scaffold. Commit.
4. **Set up CI** — GitHub Actions running the same gauntlet on push/PR. Commit.
5. **Seed `BUILD_LOG.md`** with the plan and step list.
Only now does feature work begin.

---

## 2. Roles (solo build)

- **Saurabh = Product owner + tech lead + reviewer.** Owns product decisions, approves specs and D-decisions, sets priorities, does the live/manual testing, gives the final merge call.
- **Claude = the SE department.** Writes specs (for approval), implements in green steps, runs the gauntlet, keeps the BUILD_LOG, flags tradeoffs, refuses to cut corners. Proceeds autonomously on execution; pauses for genuine product decisions or when blocked.
- **The contract:** Claude never front-loads a pile of questions; it proposes a clear recommendation with tradeoffs and moves. Saurabh redirects fast when something drifts. Neither ships slop.

---

## 3. The Spec (the contract) — what every spec must contain

A spec is signed off before its code is written. Structure:
1. **Status + type + scope** — draft/approved; feature/refactor/fix; what's in and explicitly what's NOT.
2. **Why it exists** — the problem, in plain language.
3. **Non-negotiables / invariants** — what must stay true no matter what (e.g. "no backend change," "RAG grounding preserved," "auth gates live").
4. **Locked decisions (D-numbers)** — each fork, the options, the tradeoffs, the chosen path, the rationale.
5. **The contract details** — routes, component/API contracts, data shapes, states.
6. **Build order** — numbered steps, each independently green + one commit.
7. **Test plan** — what gets unit-tested (pure logic), what's integration, what's manual.
8. **Acceptance criteria** — how we know it's done and correct.
9. **Open decisions for sign-off** — anything still needing Saurabh's call.

Master spec (`01`) is **immutable base** — it doesn't get rewritten; new specs *extend* it (`02`, `03`, …) and supersede explicitly when needed.

---

## 4. Decisions (D-numbers)

- Format: `D-<area><n>` (e.g. `D-CS1`, `D-05-2`). One decision, one line of what, plus rationale.
- Recorded in the relevant spec AND summarized in `BUILD_LOG.md`.
- A decision, once locked, isn't silently reversed — if we change our mind, that's a *new* decision that references and supersedes the old one.
- Recommendation-first: Claude presents options + a clear rec; Saurabh approves. "Go with your recs" is a valid approval once the tradeoffs are on the table.

---

## 5. The build loop (per step)

For every step in a spec's build order:
1. **Read** the spec + BUILD_LOG; reconcile what's already built against acceptance criteria.
2. **Implement** the smallest coherent slice. Pure logic goes in framework-free modules so it's unit-testable.
3. **Test** — add/extend unit tests for new pure logic; keep existing tests green.
4. **Run the gauntlet** — typecheck, lint, test, build. All green.
5. **Commit** — granular, message format `Step N.M: <what>` or `<type>(<scope>): <what>`, with the rationale and verification result in the body.
6. **Push** — to the working branch. Confirm remote == local.
7. **Update BUILD_LOG** at meaningful checkpoints (files shipped, verification, D-decisions).
Never batch five steps into one commit. Never skip the gauntlet "just this once."

---

## 6. Git & branch discipline

- **`main`** — always green, always deployable. Frozen baseline; never committed to directly for non-trivial work.
- **Feature/refactor branches** — cut from `main`, named for the work (`glowup`, `retrieval-tuning`). All risky work lives here.
- **Merge only when verified** — run the full gauntlet on the *merged* result before pushing `main`. Use `--no-ff` for a legible, revertible merge point on big features.
- **Escape hatch always intact** — you can always return to the last green `main`.
- **Clean up merged branches** — delete local + remote once fully merged (confirm `git rev-list --count main..<branch>` is 0 first).
- **Commit messages** — imperative subject; body explains *why* + verification ("gauntlet green: typecheck + lint + N tests + build").

---

## 7. Stack conventions (Java/Spring + React/TS)

### Backend — Java 21 / Spring Boot
- **Hexagonal architecture:** `domain` (models, ports, zero framework imports) → `application` (services implementing use cases) → `infrastructure` (persistence, web, adapters). Dependency arrows point inward.
- Thin controllers, fat services, typed records throughout. Sealed error hierarchies.
- Typed IDs (value objects), not raw UUIDs/strings, across the domain.
- Persistence: Flyway migrations (never auto-DDL in prod), Testcontainers for integration tests.
- Config via profiles (`application-dev.yml` etc.); secrets in `.env` (gitignored), never committed.
- No business logic in controllers; no framework types leaking into the domain layer.

### Frontend — React 19 / TypeScript / Vite
- Feature-first folders (`features/<x>/`), shared design layer (`design/`, `lib/`).
- **Pure logic extracted** into framework-free modules (`logic.ts`, `lib/ui/*`) so it's unit-tested without React.
- Design tokens in one place; **compose with existing tokens, don't invent** ad-hoc colors.
- State: local first, then a store (Zustand-class) for cross-cutting; server state via a query lib (TanStack Query).
- Strict TS — no `any` escape hatches; typecheck is part of the gate.
- Accessibility floor: visible focus rings, `prefers-reduced-motion` respected, ARIA on custom widgets, keyboard-reachable.

---

## 8. The Gauntlet (definition of done for a commit)

Backend: `mvn -B verify` (or at minimum compile + test) green.
Frontend: `typecheck` + `lint` (0 errors, 0 a11y errors) + `test` + `build` all green.
- Tests: **0 failures.** A red suite blocks the commit.
- New pure logic ships with unit tests in the same commit.
- Build must actually produce artifacts without error (warnings triaged, not ignored blindly).
- CI runs the same gauntlet — a green local + green CI is the real bar.

---

## 9. Testing philosophy

- **Unit-test the pure logic** — predicates, formatters, mappers, reducers. Framework-free, fast, deterministic. This is where most tests live.
- **Integration-test the seams** — persistence (Testcontainers), API contracts, auth.
- **Manual/live-test the journeys** — Saurabh walks the real user flows (the "gauntlet" for UX). Report bugs one at a time; resolve each before the next.
- **Timezone/locale traps:** never assert on ambiguous local-time-dependent output without pinning it.
- Tests are green or the build is red. No skipped/flaky tests left lurking.

---

## 10. Measure-before-fix (the anti-guessing rule)

When something seems wrong:
1. **Reproduce** it.
2. **Instrument** — add read-only diagnostics/logging to observe reality (no behavior change).
3. **Look at the real data** before forming the fix.
4. **Fix the actual cause**, not the first plausible-looking knob.
5. **Re-measure** to confirm.
The TAssist RAG investigation is the canonical example: measurement proved retrieval was healthy and the real issue was elsewhere — saving us from wrongly tuning a working system.

---

## 11. Anti-slop rules (keeping AI output clean)

- Every generated block must fit the **existing architecture and patterns** — no foreign idioms dropped in.
- If neither of us can explain a line, it doesn't ship.
- No dead code, no commented-out experiments left in, no "TODO: fix later" on `main` without a tracked backlog entry.
- No inventing new dependencies casually — adding a lib is a decision (weigh it, note it).
- No duplicated logic — extract and reuse.
- Prefer boring, readable, consistent code over clever code.
- When Claude catches itself about to guess, it says so and measures/asks instead.

---

## 12. BUILD_LOG.md (session continuity)

The living memory of the project. At meaningful checkpoints, append a dated entry:
- What shipped (files, features).
- Verification result (gauntlet, tests, live).
- D-decisions made, with rationale.
- Current state + what's next.
At the start of every work session: read the spec + BUILD_LOG, reconcile, then proceed. This is how a fresh session picks up without losing the thread.

---

## 13. Operational guardrails (environment)

Record project-specific "how to run things safely" so nothing gets fumbled twice. For this environment (from TAssist):
- File/shell ops on the Mac go through the reliable path (Desktop Commander), not a sandbox that can't see the real filesystem.
- Long-running processes (Spring Boot startup, test suites): fire with `nohup`, poll the log — don't block on a call that can hang.
- macOS has no `timeout`; kill stuck processes by port (`lsof -ti:PORT | xargs kill -9`) + sleep.
- `.env` sourced before backend start; JAVA_HOME/PATH/Node version set before Maven/npm.
- Frontend serves the checked-out branch; backend is a running process independent of branch — don't confuse "old UI showing" (branch) with "backend down" (process).
- Background `git push` can lose cwd / lag — verify with `git ls-remote` vs local HEAD; retry in foreground if needed.
Each new project appends its own gotchas here as they're discovered.

---

## 14. Definition of "done" for a feature

- Spec written, signed off, D-decisions locked.
- Built in green steps, each committed.
- Gauntlet green (local + CI).
- Pure logic unit-tested; journey manually verified.
- BUILD_LOG updated.
- Merged to `main` via verified `--no-ff` merge; branch cleaned up.
- No slop, no dead code, no broken windows.

---

## 15. Phase 2 / backlog hygiene

- Enhancements and "nice-to-haves" are logged in BUILD_LOG's backlog section, not smuggled into feature commits.
- A known limitation is documented (with its honest tradeoff) rather than hidden.
- "Later" is a real, written list — not a vague intention.

---

*This constitution is the reusable asset. The code is downstream of the method. Copy it into every new project, adapt §7 and §13 to the project's specifics, and hold the line on §0.*
