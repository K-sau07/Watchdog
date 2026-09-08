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
  return `median catch time today · ${hr} hr`
}
