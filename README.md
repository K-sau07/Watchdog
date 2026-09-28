# Watchdog

**A continuous agent that catches fresh job postings minutes after they go live — before the crowd.**

**[Live demo →](https://watchdog-sand.vercel.app)**  ·  API: [watchdog-api-psrv.onrender.com](https://watchdog-api-psrv.onrender.com/api/health)

> Hosted on free tiers: the API sleeps after ~15 minutes idle, so the first request after a
> quiet spell takes about 50 seconds to wake it. Everything after that is fast.


Watchdog polls company ATS boards (Greenhouse, Lever, Ashby) on a schedule, detects brand-new postings the moment they appear, filters them to exactly the roles you're hunting, and surfaces them on a cyber-futuristic radar dashboard — freshest first.

The name says it: in software, a *watchdog* is a process that continuously monitors a system and acts the instant something changes. That's exactly what this is — for the job market.

## Status

Working. All three ATS sources are implemented and polled on a schedule, the dashboard
is complete, and CI is green — 203 backend tests across 40 files plus 6 frontend test
files. Built spec-first: the master spec is `docs/01_WATCHDOG_SPEC.md`, under the
process in `docs/00_DEVELOPMENT_CONSTITUTION.md`.

## What it does

- **Polls Greenhouse, Lever and Ashby** on a schedule — each has its own client and parser
- **Detects genuinely new postings** rather than re-surfaced ones, and marks them fresh in the feed
- **Parses what the listing doesn't state plainly** — salary range, seniority, and
  **visa-sponsorship signal**, extracted from the title and description
- **Filters** by role, include/exclude keywords, location, posting age and US-only
- **Live agent status** showing whether the poller is healthy and the median catch time today
- **Retention** — old postings are aged out on a schedule

## Architecture

Hexagonal: `domain` holds the model and ports, `application` the matching and status
services, `infrastructure` the ATS adapters, persistence and web layer. Persistence is
`JdbcTemplate` against Flyway-managed migrations rather than JPA — the queries are
read-heavy and shaped for the feed.

| | |
|:--|:--|
| `GET /api/postings` | the filtered feed |
| `GET /api/agent/status` | poller health and median catch time |
| `POST /api/agent/poll` | trigger a poll run |
| `GET /api/filter-profiles` | saved filters |
| `GET /health` | liveness |

## Stack
Java 21 · Spring Boot (hexagonal) · PostgreSQL + Flyway · Redis (ShedLock) · scheduled poller
React 19 · Vite · TypeScript · Tailwind

## Thesis
The early-applicant advantage is real. By the time a role hits LinkedIn/Indeed, hundreds have applied. Watchdog reads where jobs are *born* — the company ATS — so you see them first. Speed is the product.
