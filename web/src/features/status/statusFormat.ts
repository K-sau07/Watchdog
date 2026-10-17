/**
 * Pure formatting for the agent-status bar (framework-free, unit-tested). Relative time
 * strings are computed against a caller-supplied "now" so they're deterministic in tests.
 */

/** "38s ago" / "1m ago" / "2h ago" / "—" (never polled). Past-relative to now. */
export function agoLabel(iso: string | null, nowMs: number): string {
  if (!iso) return '—'
  const secs = Math.max(0, Math.round((nowMs - Date.parse(iso)) / 1000))
  if (secs < 60) return `${secs}s ago`
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`
  return `${Math.floor(secs / 3600)}h ago`
}

/** "in 1:22" (m:ss) / "now" when due/overdue / "—" when unknown. Future-relative. */
export function countdownLabel(iso: string | null, nowMs: number): string {
  if (!iso) return '—'
  const secs = Math.round((Date.parse(iso) - nowMs) / 1000)
  if (secs <= 0) return 'now'
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `in ${m}:${String(s).padStart(2, '0')}`
}

/** The signature headline: "median catch time today · 6 min", honest when unknown. */
export function medianLabel(minutes: number | null): string {
  if (minutes === null) return 'no catches timed yet'
  if (minutes < 1) return 'median catch time today · <1 min'
  if (minutes < 60) return `median catch time today · ${minutes} min`
  const hr = Math.floor(minutes / 60)
  // Past two days, hours stop reading as a duration — "218 hr" is noise. Roll up to days.
  if (hr < 48) return `median catch time today · ${hr} hr`
  const days = Math.floor(hr / 24)
  return `median catch time today · ${days} ${days === 1 ? 'day' : 'days'}`
}

/**
 * Is the catch time fast enough to claim an early-applicant advantage?
 *
 * The headline used to assert "you're seeing these before most applicants" whenever a
 * median existed — including at 218 hr, which is nine days and the opposite of the claim.
 */
export const EARLY_ADVANTAGE_MAX_MINUTES = 120

export function hasEarlyAdvantage(minutes: number | null): boolean {
  return minutes !== null && minutes <= EARLY_ADVANTAGE_MAX_MINUTES
}

/** What the biggest type on the page should say, and the quiet line under it. */
export interface Hero {
  /** The number itself. */
  big: string;
  /** What the number is. */
  label: string;
  /** Shown only when the headline needs explaining; null otherwise. */
  note: string | null;
}

/**
 * The headline stat, chosen by what the data can actually support.
 *
 * Catch-time is the product's whole claim, so it leads whenever there is a real one to
 * report. But on a day that was all backfill there are no genuine catches, and the old
 * behaviour — median over every row written today — put "88 days" in 40px type. The
 * number was arithmetically right and told the reader the opposite of the truth.
 *
 * So when nothing was caught, the headline falls back to a number that is still real
 * and still the agent's own work: how many postings it wrote today. The note says why
 * catch-time is missing rather than leaving a hole.
 */
export function heroStat(
  medianMinutes: number | null,
  caughtToday: number,
  newToday: number,
): Hero {
  if (medianMinutes !== null && caughtToday > 0) {
    const [big, label] = splitMedianLabel(medianLabel(medianMinutes));
    return { big, label, note: null };
  }
  return {
    big: newToday.toLocaleString('en-US'),
    label: newToday === 1 ? 'posting found today' : 'postings found today',
    note:
      newToday > 0
        ? 'catch time starts counting once a role is posted while the agent is already watching that board'
        : null,
  };
}

/** "median catch time today · 6 min" → ["6 min", "median catch time today"]. */
function splitMedianLabel(label: string): [string, string] {
  const parts = label.split(' · ');
  return parts.length === 2 ? [parts[1], parts[0]] : [label, ''];
}
