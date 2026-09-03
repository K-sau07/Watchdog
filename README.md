# Watchdog

**A continuous agent that catches fresh job postings minutes after they go live — before the crowd.**

Watchdog polls company ATS boards (Greenhouse, Lever, Ashby) on a schedule, detects brand-new postings the moment they appear, filters them to exactly the roles you're hunting, and surfaces them on a cyber-futuristic radar dashboard — freshest first.

The name says it: in software, a *watchdog* is a process that continuously monitors a system and acts the instant something changes. That's exactly what this is — for the job market.

## Status
🚧 Early development. Master spec in `docs/01_WATCHDOG_SPEC.md`. Built under the process in `docs/00_DEVELOPMENT_CONSTITUTION.md` (spec-first, green-gate, no slop).

## Stack
Java 21 / Spring Boot (hexagonal) · React 19 / Vite / TypeScript / Tailwind · PostgreSQL · Redis · scheduled poller

## Thesis
The early-applicant advantage is real. By the time a role hits LinkedIn/Indeed, hundreds have applied. Watchdog reads where jobs are *born* — the company ATS — so you see them first. Speed is the product.
