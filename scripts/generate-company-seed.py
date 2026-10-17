#!/usr/bin/env python3
"""
Regenerate api/src/main/resources/seed/companies-seed.json from the YC company
directory, resolving each company to a *verified* ATS board.

Why this exists
---------------
The seed used to be hand-curated (134 rows, mostly late-stage: Stripe, Airbnb,
Dropbox). Watchdog's thesis is catching postings at companies that are still
small enough for one application to matter, so the seed should be early- and
growth-stage startups instead.

Sources, both free and first-party:
  * YC directory   https://yc-oss.github.io/api/  (public JSON mirror of
                   ycombinator.com/companies; carries stage/status/team_size)
  * ATS boards     Greenhouse / Lever / Ashby public job-board APIs, which those
                   vendors publish so companies can embed their own boards.

Verification is the whole game
------------------------------
Guessing a slug from a company name produces collisions that are worse than a
miss: `pulse` on Greenhouse is "Pulse Healthcare", a UK staffing firm with ~2700
nursing roles. Unverified probing scored 65% "hits"; with verification on it is
~42%, and those are real. We take the lower number.

  Greenhouse  verify via /v1/boards/{slug} -> board name vs YC name/domain
  Ashby       no board name, so verify the job URLs carry the slug or domain
  Lever       accepted on a 200 with postings (slugs are not generic enough to
              collide in practice; revisit if that stops being true)

Usage
-----
  ./scripts/generate-company-seed.py                 # write the seed file
  ./scripts/generate-company-seed.py --dry-run       # report, write nothing
  ./scripts/generate-company-seed.py --limit 50      # quick sample run
  ./scripts/generate-company-seed.py --keep-existing # union with current seed

Stdlib only, to match verify-ats-boards.sh. Takes a few minutes for the full
cohort; results cache to .seed-cache.json so reruns are cheap.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

YC_FEED = "https://yc-oss.github.io/api/companies/all.json"
REPO = Path(__file__).resolve().parent.parent
SEED_FILE = REPO / "api/src/main/resources/seed/companies-seed.json"
CACHE_FILE = Path(__file__).resolve().parent / ".seed-cache.json"

# Identify ourselves. Greenhouse/Lever/Ashby are fine with this volume, but an
# anonymous scraper-looking agent is how you get a 403 you deserve.
UA = "watchdog-seed-generator/1.0 (+https://github.com/K-sau07/Watchdog)"

# Team-size window. Below this a company rarely has a real board; above it we
# are back to the late-stage companies this change exists to get away from.
MIN_TEAM = 11
MAX_TEAM = 500

TIMEOUT = 12
WORKERS = 6


# --------------------------------------------------------------------------
# http
# --------------------------------------------------------------------------

def fetch_json(url: str, retries: int = 2, timeout: int = TIMEOUT):
    """GET -> parsed JSON, or None for any non-200 / parse failure.

    A miss is the common case here (most slug guesses are wrong), so a failure
    is not exceptional and must stay cheap. The YC feed is the exception: it is
    ~10 MB and needs a longer leash than a slug probe.
    """
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                if resp.status != 200:
                    return None
                return json.load(resp)
        except urllib.error.HTTPError as e:
            if e.code in (429, 503) and attempt < retries:
                time.sleep(2 ** attempt)
                continue
            return None
        except Exception:
            if attempt < retries:
                time.sleep(0.5)
                continue
            return None
    return None


# --------------------------------------------------------------------------
# slug candidates
# --------------------------------------------------------------------------

def norm(s: str | None) -> str:
    return re.sub(r"[^a-z0-9]", "", (s or "").lower())


def domain_of(website: str | None) -> str:
    """Bare second-level domain: https://www.acme-ai.com/careers -> acmeai."""
    if not website:
        return ""
    host = website.split("//")[-1].split("/")[0]
    host = re.sub(r"^www\.", "", host)
    return norm(host.split(".")[0])


def host_of(website: str | None) -> str:
    """Full registrable host, suffix included: https://www.tempo.new/x -> tempo.new.

    The suffix is load-bearing. YC's Tempo is tempo.new; the Greenhouse board
    named "Tempo" links to tempoenergy.com. Comparing bare labels calls those a
    match ("tempo" is a substring of "tempoenergy"); comparing full hosts does
    not. Every generic-name false positive we found dies on this distinction.
    """
    if not website:
        return ""
    host = website.split("//")[-1].split("/")[0].lower()
    host = re.sub(r"^www\.", "", host)
    return host.split(":")[0].strip()


def slug_candidates(company: dict) -> list[str]:
    """Plausible board slugs, most likely first.

    YC's own slug is the best guess; the display name is second. Companies
    routinely drop a suffix ("Acme Labs" -> acme) when registering a board.
    """
    out: list[str] = []
    for base in (company.get("slug"), company.get("name"), company.get("website")):
        if not base:
            continue
        if base == company.get("website"):
            out.append(domain_of(base))
            continue
        out.append(norm(base))
        out.append(re.sub(r"[^a-z0-9]+", "-", base.lower()).strip("-"))

    for cand in list(out):
        for suffix in ("inc", "labs", "ai", "hq", "technologies", "tech", "io", "app"):
            if cand.endswith(suffix) and len(cand) > len(suffix) + 2:
                out.append(cand[: -len(suffix)])

    return [c for c in dict.fromkeys(out) if c and len(c) > 1][:5]


def identity_matches(board_name: str, company: dict) -> bool:
    """Does this board actually belong to this company?

    Deliberately strict. A false positive pollutes the feed with hundreds of
    unrelated roles; a false negative just costs us one company.
    """
    board = norm(board_name)
    name = norm(company.get("name"))
    dom = domain_of(company.get("website"))
    if not board or not name:
        return False
    if board == name or board == dom:
        return True
    # Containment, but only when the shorter side is long enough to be meaningful.
    # "pulse" vs "pulsehealthcare" must NOT match; "novacredit" vs "nova credit" must.
    for a, b in ((board, name), (board, dom)):
        if not b:
            continue
        short, long = sorted((a, b), key=len)
        if len(short) >= 6 and short in long:
            return True
    return False


# --------------------------------------------------------------------------
# ats resolution
# --------------------------------------------------------------------------

# A name match alone is enough only when the name is distinctive. Below this
# length, generic words ("hive", "lob", "magic", "reach") collide across
# unrelated companies and we require the company's own host to appear in the
# board's content before believing it.
DISTINCTIVE_NAME_LEN = 8


def corroborated_by_host(blob: str, company: dict) -> bool:
    """Does the board's own content link back to the company's website?"""
    host = host_of(company.get("website"))
    return bool(host) and host in blob.lower()


def corroborated_by_slug_and_name(slug: str, blob: str, company: dict) -> bool:
    """Two independent signals agreeing, for boards that carry no host link.

    Ashby and Lever URLs embed the board slug rather than the company's own
    domain, so host corroboration never fires for them. Instead require both:

      1. the slug is exactly the company's domain label (kombo <-> kombo.dev)
      2. the company's name appears in the posting text

    Either alone is weak -- a name recurs across unrelated companies, and a
    domain label can collide across TLDs. Together they are strong enough, and
    unlike a name-length threshold they do not punish a company for being
    called Lapel instead of Fieldguide. Short names were 60% of the cohort.
    """
    if not slug or slug != domain_of(company.get("website")):
        return False
    name = norm(company.get("name"))
    return bool(name) and name in norm(blob)


# How well we proved the board belongs to the company. Not cosmetic: the two
# tiers fail differently and the caller should be able to act on the difference.
#
#   HOST_VERIFIED     the board's own content links to the company's website.
#                     Effectively conclusive.
#   SLUG_NAME_MATCH   the slug equals the company's domain label AND the name
#                     appears in the postings. Usually right, but it cannot
#                     separate a same-label/same-name collision: YC's Hive is
#                     hive.co, and Lever's "hive" board is thehive.ai, which
#                     satisfies both signals while being a different company.
#
# Everything is seeded; the field exists so a bad row can be found later rather
# than silently polluting the feed.
HOST_VERIFIED = "HOST_VERIFIED"
SLUG_NAME_MATCH = "SLUG_NAME_MATCH"


def try_greenhouse(slug: str, company: dict):
    meta = fetch_json(f"https://boards-api.greenhouse.io/v1/boards/{slug}")
    if not meta or not meta.get("name"):
        return None
    if not identity_matches(meta["name"], company):
        return None  # a real board, but someone else's ("pulse" -> Pulse Healthcare)

    jobs_body = fetch_json(f"https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true")
    jobs = (jobs_body or {}).get("jobs") or []
    count = (jobs_body or {}).get("meta", {}).get("total", len(jobs))

    # An empty board is worthless to the poller and leaves us nothing to
    # corroborate against, so it never earns a seed row.
    if count == 0:
        return None

    blob = json.dumps(meta) + json.dumps(jobs[:5])
    if corroborated_by_host(blob, company):
        return ("GREENHOUSE", slug, count, meta["name"], HOST_VERIFIED)
    if len(norm(company.get("name"))) >= DISTINCTIVE_NAME_LEN:
        return ("GREENHOUSE", slug, count, meta["name"], SLUG_NAME_MATCH)
    return None


def try_ashby(slug: str, company: dict):
    body = fetch_json(f"https://api.ashbyhq.com/posting-api/job-board/{slug}")
    jobs = (body or {}).get("jobs") or []
    if not jobs:
        return None
    # Ashby exposes no board name, so the job payloads have to carry the proof.
    blob = json.dumps(jobs[:5])
    if corroborated_by_host(blob, company):
        return ("ASHBY", slug, len(jobs), company.get("name"), HOST_VERIFIED)
    if corroborated_by_slug_and_name(slug, blob, company):
        return ("ASHBY", slug, len(jobs), company.get("name"), SLUG_NAME_MATCH)
    return None


def try_lever(slug: str, company: dict):
    body = fetch_json(f"https://api.lever.co/v0/postings/{slug}?mode=json")
    if not isinstance(body, list) or not body:
        return None
    blob = json.dumps(body[:5])
    if corroborated_by_host(blob, company):
        return ("LEVER", slug, len(body), company.get("name"), HOST_VERIFIED)
    if corroborated_by_slug_and_name(slug, blob, company):
        return ("LEVER", slug, len(body), company.get("name"), SLUG_NAME_MATCH)
    return None


def resolve(company: dict):
    """First verified board wins. Returns a seed row, or None."""
    for slug in slug_candidates(company):
        for probe in (try_greenhouse, try_ashby, try_lever):
            hit = probe(slug, company)
            if hit:
                source, resolved_slug, job_count, board_name, match_confidence = hit
                return {
                    "name": company["name"],
                    "source": source,
                    "slug": resolved_slug,
                    "stage": map_stage(company),
                    "stageSource": "YC_DIRECTORY",
                    "stageAsOf": date.today().isoformat(),
                    "matchConfidence": match_confidence,
                    "_jobs": job_count,
                    "_board": board_name,
                    "_team": company.get("team_size"),
                }
    return None


def map_stage(company: dict) -> str:
    """YC publishes only Early/Growth, not series letters.

    This is an honest proxy, not a funding fact: Early covers pre-seed through
    roughly Series A, Growth is roughly Series B and later. Team size sharpens
    it a little. If a real funding source is ever wired in, it should overwrite
    this and set stageSource accordingly.
    """
    stage = company.get("stage")
    team = company.get("team_size") or 0
    if stage == "Growth":
        return "GROWTH" if team > 200 else "SERIES_B_PLUS"
    if stage == "Early":
        return "SEED" if team < 25 else "SERIES_A"
    return "UNKNOWN"


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------

def load_cohort(limit: int | None):
    print("fetching YC directory ... ", end="", flush=True)
    companies = fetch_json(YC_FEED, timeout=120)  # ~10 MB
    if not companies:
        sys.exit("could not fetch the YC feed")
    print(f"{len(companies)} companies", flush=True)

    cohort = [
        c for c in companies
        if c.get("isHiring")
        and c.get("status") == "Active"
        and c.get("stage") in ("Early", "Growth")
        and MIN_TEAM <= (c.get("team_size") or 0) <= MAX_TEAM
        and not c.get("nonprofit")
        and c.get("slug") != "y-combinator"  # YC lists itself; not a target
    ]
    print(f"cohort (active, hiring, early/growth, team {MIN_TEAM}-{MAX_TEAM}): {len(cohort)}")
    return cohort[:limit] if limit else cohort


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dry-run", action="store_true", help="report only, write nothing")
    ap.add_argument("--limit", type=int, help="only process the first N (for a quick sample)")
    ap.add_argument("--keep-existing", action="store_true",
                    help="union with the current seed instead of replacing it")
    ap.add_argument("--no-cache", action="store_true", help="ignore the resolution cache")
    args = ap.parse_args()

    cohort = load_cohort(args.limit)

    cache = {}
    if CACHE_FILE.exists() and not args.no_cache:
        try:
            cache = json.loads(CACHE_FILE.read_text())
            print(f"cache: {len(cache)} previously resolved")
        except Exception:
            cache = {}

    todo = [c for c in cohort if str(c["id"]) not in cache]
    print(f"probing {len(todo)} companies across Greenhouse/Ashby/Lever ...\n")

    resolved, done = [], 0
    with concurrent.futures.ThreadPoolExecutor(WORKERS) as pool:
        futures = {pool.submit(resolve, c): c for c in todo}
        for fut in concurrent.futures.as_completed(futures):
            company = futures[fut]
            done += 1
            try:
                row = fut.result()
            except Exception as e:
                print(f"  !! {company['name']}: {e}", file=sys.stderr)
                row = None
            cache[str(company["id"])] = row
            if row:
                resolved.append(row)
                print(f"  [{done:>4}/{len(todo)}] {row['name'][:28]:<28} "
                      f"{row['source']:<11} {row['slug'][:22]:<22} {row['_jobs']:>4} jobs")
            elif done % 25 == 0:
                print(f"  [{done:>4}/{len(todo)}] ...")

    for key, row in cache.items():
        if row and row not in resolved:
            resolved.append(row)

    if not args.no_cache:
        CACHE_FILE.write_text(json.dumps(cache, indent=1))

    attempted = len(cohort)
    print(f"\nresolved {len(resolved)}/{attempted} "
          f"({len(resolved) / max(attempted, 1) * 100:.0f}% verified)")
    print(f"total open roles: {sum(r['_jobs'] for r in resolved)}")

    by_source: dict[str, int] = {}
    by_stage: dict[str, int] = {}
    for r in resolved:
        by_source[r["source"]] = by_source.get(r["source"], 0) + 1
        by_stage[r["stage"]] = by_stage.get(r["stage"], 0) + 1
    print(f"by source: {by_source}")
    print(f"by stage:  {by_stage}")

    rows = [{k: v for k, v in r.items() if not k.startswith("_")} for r in resolved]

    if args.keep_existing and SEED_FILE.exists():
        existing = json.loads(SEED_FILE.read_text()).get("companies", [])
        seen = {(r["source"], r["slug"]) for r in rows}
        for e in existing:
            if (e.get("source"), e.get("slug")) not in seen:
                e.setdefault("stage", "UNKNOWN")
                e.setdefault("stageSource", "MANUAL")
                e.setdefault("stageAsOf", date.today().isoformat())
                rows.append(e)
        print(f"kept {len(rows) - len(resolved)} existing rows")

    rows.sort(key=lambda r: r["name"].lower())

    if args.dry_run:
        print("\n--dry-run: nothing written")
        return 0

    SEED_FILE.write_text(json.dumps({"companies": rows}, indent=2) + "\n")
    print(f"\nwrote {len(rows)} companies -> {SEED_FILE.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
