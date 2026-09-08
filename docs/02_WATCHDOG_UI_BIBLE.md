# 02 — Watchdog UI Bible

**Status:** DRAFT for sign-off · **Type:** Design system (extends `01_WATCHDOG_SPEC.md`, governs S8 UI) · **Governed by:** `00_DEVELOPMENT_CONSTITUTION.md`
**Gate (D-WD4):** No UI code ships before this is approved. This document is the contract for S8.

> **The brief in one line:** a command console for a job hunter who wins by being *first*. Not a NOC for machines — a cockpit for one human racing a clock. Speed is the product; the UI's job is to make "you caught this 4 minutes after it posted" *feel* like an edge.

---

## 0. Decisions locked for the UI

- **G-UI0 — Accent = signal amber** (+ a hotter amber reserved for the freshest-catch pulse). One accent family, two temperatures. Chosen over neon-green / electric-cyan, which the 2026 command-console template pool has converged on — using either would read as a generic NOC template on sight. Amber is command-console-legitimate, semantically right ("a fresh thing lit up, act now" — warmer/more urgent-to-act than passive cyan), and on-thesis for speed. Everything else is neutral.
- **G-UI1 — Type = humanist-technical.** A grotesk display with real character + a clean humanist body + a monospace used *only* for time and numeric data (timestamps, catch-time, salary, counts). This follows the 2026 "tool for humans, not a machine" counter-trend and deliberately avoids the Orbitron/Share-Tech sci-fi default. Mono as everywhere-body would make it feel like a terminal — it's an accent, not the voice.
- **G-UI2 — 90/10 calm-to-signal.** ~90% of the screen is calm neutral; amber appears only for freshness and state. Borrowed from real NOC practice — it's the one functional principle worth keeping. Restraint is what makes it classy vs. gaudy (spec §6).
- **G-UI3 — Radar is a concept, not a graphic.** The "radar" is the live freshness-ordered feed with a pulse on new arrivals — NOT a skeuomorphic sweeping-line/green-CRT radar dial. The literal radar sweep is the single most overused command-console cliché; we evoke "actively watching" through motion and ordering, not a decorative dial.
- **G-UI4 — Dark-first is justified** (spec §6): a monitoring console viewed for long stretches, glanceable in low light. Not an inverted light theme — designed dark from the base up. No light mode in v1.

---

## 1. What the user sees first (the hero)

Not a big-number-over-gradient (the generic default). The hero is the **live feed itself, already working** — the newest posting pulsing at the top the instant the page loads — with a single honest headline stat riding above it:

> **median catch time today · 6 min**  ·  *you're seeing these before most applicants*

The most characteristic thing in Watchdog's world is a fresh job arriving. So the first thing the eye lands on is a real card, freshness-glowing, with its "caught 4 min after posting" stamp — the product's soul, on screen, in the first second. The stat is proof, not decoration; if there's no data yet, it says so plainly (see §7 empty states).

---

## 2. Color tokens

Deep-charcoal neutral base (not pure black — pure black on OLED causes halation/smear on moving content, and the research is explicit: use deep greys, not #000). Amber is the only chroma; everything else is a neutral step.

```
--bg-void        #0A0B0D   page background (near-black charcoal, faint blue-cool cast)
--bg-surface     #121418   panels, filter rail, top bar
--bg-card        #16191E   job card resting surface
--bg-card-hover  #1C2027   job card hover / focus
--line           #24282F   hairline borders, dividers, grid rules
--line-strong    #333941   emphasized dividers
--text-hi        #ECEEF2   primary text (titles, headline numbers)
--text-mid       #A7ADB8   secondary text (labels, meta)
--text-lo        #6B717C   tertiary (timestamps at rest, disabled)

--amber          #FFB020   THE accent — freshness, active state, primary actions
--amber-hot      #FFD36B   freshest-catch pulse (<2 min); brightest, transient only
--amber-dim      #B87A16   amber at rest / low-emphasis amber (borders, applied state)
--amber-glow     rgba(255,176,32,0.16)   glow wash behind fresh cards (used sparingly)

Semantic (state, muted on purpose — never competes with amber):
--ok             #4DB27A   sponsorship OFFERED badge
--warn           #C77D3A   (reserved; rate-limit / degraded agent)
--danger         #C7534A   sponsorship NOT_OFFERED badge, hide confirm
--neutral-badge  #2A2F37   UNKNOWN / source badges (fill), text in --text-mid
```

**Usage law (G-UI2):** amber is spent on (1) the freshness glow/pulse of new cards, (2) the active/selected state of filters, (3) primary action affordances. Nowhere else. A screen at rest with nothing fresh is almost entirely neutral — and that's correct. When a job lands, amber is the thing that moves.

**Freshness ramp** (drives card glow + the catch stamp color), by catch-time or age-in-feed:
```
< 2 min   --amber-hot   + glow, brief pulse animation on entry
2–15 min  --amber       steady, subtle glow
15–60 min --amber-dim   no glow, amber text only
> 1 hr    --text-mid    fully cooled to neutral; it's not "fresh" anymore
```
This is the signature stat made physical: newer literally burns hotter. Honest (spec §8.4) — a card with no `postedAt` shows a neutral "caught —" and never fakes a temperature.

---

## 3. Typography

Three roles, each earning its place (G-UI1). All Google/OFL, embeddable, free — matches the S0 Tailwind-v4 CSS-first setup.

```
Display  "Space Grotesk"  — headline numbers, the median-catch stat, section heads.
                            Grotesk with quirk = technical character without Orbitron cosplay.
Body     "Inter"          — everything readable: titles, descriptions, labels, buttons.
                            Neutral, high x-height, tabular-figure capable, proven at small sizes.
Mono     "JetBrains Mono" — ONLY: timestamps, "caught N min", salary, counts, IDs, source slugs.
                            The precision signal. Never body copy (would read as terminal).
```

**Humanist touch (the "tool for humans" angle):** one restrained serif accent — *not* a second workhorse. `"Fraunces"` (optical, characterful) is used for exactly one thing: the reflective headline line beside the stat (e.g. *"you're seeing these before most applicants"*). It signals a human's tool, once, and never appears in chrome. If it feels like an accessory too many at build time, cut it (Chanel rule) — the design must survive without it.

**Type scale** (1.250 major-third, base 16px):
```
stat-xl   40 / 44   Space Grotesk 600   the median-catch headline number
h1        31 / 38   Space Grotesk 600   view titles
h2        25 / 32   Space Grotesk 500
title     20 / 28   Inter 600           job card title
body      16 / 24   Inter 400
label     14 / 20   Inter 500           filter labels, meta
data      13 / 18   JetBrains Mono 500  timestamps, catch-time, salary
micro     12 / 16   Inter 500           badges (sentence case, NOT all-caps)
```

**Tells we refuse** (frontend-design skill): no ALL-CAPS tracked-out eyebrow labels; no `A · B · C` middle-dot meta strings; no `→` appended to buttons; no single-word-accent headlines; mono is data-only, never a label affectation.

---

## 4. Layout

Three zones (spec §6): filter rail left, live feed center, agent-status bar top. Command-console composition, but calm.

```
┌────────────────────────────────────────────────────────────────────────┐
│  WATCHDOG        ● watching · 25 boards   last poll 0:38 ago   ⏱ 6 min   │  agent bar
├───────────────┬────────────────────────────────────────────────────────┤
│  FILTERS      │   median catch time today · 6 min                        │
│               │   you're seeing these before most applicants             │  hero stat
│  role         │  ┌──────────────────────────────────────────────────┐   │
│  [ new grad ] │  │ ◉ Software Engineer, New Grad      caught 2 min ↑ │   │  ← hot pulse
│  seniority    │  │   Ramp · New York / Remote · full-time            │   │
│  keywords     │  │   $120k–150k · sponsorship ✓        [save][✓][×]  │   │
│  location     │  └──────────────────────────────────────────────────┘   │
│  salary       │  ┌──────────────────────────────────────────────────┐   │
│  sponsorship  │  │   Backend Engineer I              caught 11 min   │   │  ← steady amber
│  posted within│  │   Linear · Remote · full-time                     │   │
│  source       │  │   sponsorship —                    [save][✓][×]   │   │
│  ─────────    │  └──────────────────────────────────────────────────┘   │
│  [ reset ]    │  ┌──────────────────────────────────────────────────┐   │
│               │  │   Software Engineer II            caught 3 hr     │   │  ← cooled/neutral
│               │  │   Vanta · San Francisco · full-time               │   │
│               │  └──────────────────────────────────────────────────┘   │
└───────────────┴────────────────────────────────────────────────────────┘
```

- **Alignment:** left-aligned throughout. Data is scanned top-to-bottom, newest-first; centered text would fight the scan. Numbers (catch-time, salary) are right-aligned within the card so they form a scannable column.
- **Filter rail:** collapsible (spec §6). Fixed width ~280px desktop; becomes a top drawer on mobile. Every §5 dimension present; active filters show an amber left-edge tick.
- **Feed:** single column of cards, generous vertical rhythm, `first_seen_at DESC`. Not a grid of identical SaaS cards (frontend-design cliché #4) — a *stream*, with hierarchy by freshness, not uniform tiles.
- **Grid/HUD motif, restrained:** a faint 1px `--line` baseline grid on the agent bar only; thin rules, no glowing panel borders everywhere (that's the NOC cliché). The one "HUD" flourish is the live pulse dot in the agent bar.
- **Responsive:** rail → drawer at <900px; card meta wraps; catch-time stays visible (it's the point). Quality floor per constitution §7: visible focus rings, `prefers-reduced-motion` honored, keyboard-reachable, WCAG-AA contrast (amber #FFB020 on #16191E passes for large/again for the glow-independent text — text never relies on glow alone).

---

## 5. The job card (the atom)

Spec §6 fields, arranged for a 2-second scan: *is it fresh? is it me? can I apply?*

```
┌────────────────────────────────────────────────────────────────┐
│ ◉  Software Engineer, New Grad                    caught 2 min ↑ │   row 1: title + STAMP
│    Ramp  ·  New York / Remote  ·  full-time                      │   row 2: company/loc/type
│    $120k–150k   sponsorship ✓   new grad   [GH]                  │   row 3: badges (data mono)
│                                        [ save ]  [ applied ]  [×]│   row 4: actions
└────────────────────────────────────────────────────────────────┘
```

- **`◉` freshness dot + left glow:** color/intensity from the §2 freshness ramp. The only animated element (see §6).
- **Catch stamp** (`caught 2 min`): JetBrains Mono, amber-temped, top-right — the signature stat, always the most prominent data on the card. Shows `caught —` honestly when `postedAt` is unknown.
- **Badges** (row 3): sentence case, mono for the numeric one (salary). `sponsorship ✓` = `--ok`, `✗` = `--danger`, `—` = neutral. Source badge `[GH]/[LV]/[AS]` in `--neutral-badge`. Seniority as a plain word, no chip-noise.
- **Actions:** `save` (outline), `applied` (fills amber-dim when set), `×` hide (ghost until hover→`--danger`). Wired to `PUT /api/postings/{id}/state`. Verbs stay consistent through the flow (frontend-design writing rule): the button says "applied," the state reads "applied."
- **State treatments:** SAVED → subtle amber left-border persists. APPLIED → card recedes slightly (dim -8% lum) + a small mono `applied 2:14pm`. HIDDEN → card animates out of the feed (collapse+fade, reduced-motion: instant remove).

---

## 6. Motion (one orchestrated moment, not scattered effects)

The frontend-design rule: a single deliberate moment beats fade-slide-up on everything.

- **The one moment — a fresh arrival.** When a new posting enters at the top: it slides in (8px) and the freshness glow pulses hot→steady once over ~1.2s, then holds steady. This is the "radar caught something" beat. Everything else is still.
- **State actions** answer the user: hide = collapse+fade (shows what left); applied = quick amber fill. These are feedback, not decoration — allowed.
- **The live pulse dot** in the agent bar breathes slowly (2s) whenever the agent is polling — the "it's working for you" heartbeat. The only ambient motion.
- **`prefers-reduced-motion`:** all of the above degrade to instant state changes; the glow becomes a static color, no pulse, no slide. Nothing important is conveyed by motion alone.

No hover-lift on every card. No section entrance animations. No background particle/scanline drift (the gaudy tell). Restraint is the brief.

---

## 7. Copy & states (words are design content)

The interface's voice: plain, active, confident, never chatty. Sentence case everywhere. Copy is written from the hunter's view, not the system's.

- **Agent bar:** `● watching · 25 boards` / `last poll 38s ago` / `next in 1:22`. When degraded: `● paused` or `● retrying — a board rate-limited us` (honest, in the interface's voice, no apology — frontend-design failure-state rule).
- **Hero stat, has data:** `median catch time today · 6 min` + `you're seeing these before most applicants`. **No data yet:** `no catches yet today` + `the agent is watching — new roles will surface here the moment they post`. An empty screen is an invitation, not a blank.
- **Empty feed (filters too tight):** `nothing matches these filters` + `[loosen filters]` (one action, does the obvious thing). Not "0 results."
- **Catch stamp:** `caught 2 min` / `caught 3 hr` / `caught —` (unknown). Never faked.
- **Errors** name what happened + the fix: `couldn't reach the server — retrying` with a manual `retry`. Never vague, never a persona apology.
- **Actions keep their verb through the flow:** `save`→toast `saved`; `applied`→`marked applied`; `×`/hide→`hidden` with a 5s `undo`.

---

## 8. Component inventory (what S8 builds, in order)

Maps to the S8 build sub-steps. Each is a framework-free-logic-where-possible React component (constitution §7: pure logic in `logic.ts`, unit-tested without React).

1. **Design tokens** — the §2/§3 values as CSS custom properties + Tailwind v4 `@theme` (CSS-first, no config file — S0.2 setup). Fonts loaded. *No component yet; the foundation.*
2. **`freshness.ts` (pure)** — catch-minutes → ramp bucket (hot/steady/dim/cooled) + label (`caught N min`). Unit-tested; this is the signature stat's brain, framework-free.
3. **`AgentStatusBar`** — consumes `GET /api/agent/status`; live pulse; poll countdown.
4. **`JobCard`** — the §5 atom; freshness dot/glow from `freshness.ts`; badges; state actions → `PUT /api/postings/{id}/state`.
5. **`FeedList`** — newest-first stream; consumes `GET /api/postings`; new-arrival motion; empty/loading/error states.
6. **`FilterRail`** — every §5 dimension → query params; active-state ticks; reset; collapsible/drawer. Loads/saves via `GET|PUT /api/filter-profiles/default`.
7. **`HeroStat`** — median-catch headline + reflective line; the §1 hero.
8. **Wire-up + `App` shell** — three-zone layout; data fetching (TanStack Query per constitution §7); reduced-motion + a11y pass; full gauntlet.

Note: `GET /api/agent/status` is the one spec §7 endpoint not yet built (S6/S7 covered the rest). It's needed for component 3 — **flag: a small S8 backend sub-step adds it** (last poll, next poll, boards polled, new-today count, median catch-time today) before `AgentStatusBar`. Called out here so it's not a surprise mid-S8.

---

## 9. Deliberately NOT doing (anti-cliché ledger)

On record so we can hold the line (and so a reviewer sees the reasoning):

- **No literal radar dial / sweep line.** Radar is the live feed concept (G-UI3).
- **No neon-green or cyan on black.** The template-pool default; amber instead (G-UI0).
- **No Orbitron / Share Tech / sci-fi display face.** Humanist-technical instead (G-UI1).
- **No mono as body text.** Data-only, or it's a terminal, not a tool.
- **No glowing borders on every panel, no scanline/particle background.** Gaudy tells; glow is reserved for freshness only.
- **No uniform SaaS card grid** with one radius + one grey shadow on everything. A stream with freshness hierarchy.
- **No ALL-CAPS eyebrows, `·`-joined meta, `→` on buttons, single-word-accent headlines.** (frontend-design tells.)
- **No light-mode invert.** Designed dark from the base (G-UI4).

---

## 10. Acceptance (UI done, for S9)

- Matches these tokens/type/layout; the freshness ramp is visible and honest.
- The signature stat (`caught N min`) is prominent, real, and cooled correctly by age.
- ~90/10 calm-to-amber holds; a resting screen is nearly neutral.
- Filters drive all §5 dimensions server-side; profile loads/saves.
- Save/Applied/Hide work and animate per §5/§6; hidden leaves the feed.
- Reduced-motion, keyboard, and AA-contrast floors met.
- Reads as *Watchdog*, not a command-console template — a stranger shouldn't mistake it for the Signal/NOC kit.

---

*End of draft. Per D-WD4: sign off before any S8 UI code. Open axes intentionally left to build-time judgement: exact glow radii, card padding rhythm, the Fraunces keep/cut call (Chanel rule at build).*
