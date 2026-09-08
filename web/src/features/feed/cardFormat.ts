import type { PostingSummary, SalaryDto } from '../../lib/api'

/**
 * Pure presentation helpers for a job card (framework-free, unit-tested). Formatting logic
 * lives here so JobCard stays a thin render and the string rules are verified without React.
 */

/** "$120k–150k" / "$120k+" / "up to $150k" / null when no salary. Compact, k-suffixed. */
export function formatSalary(s: SalaryDto | null): string | null {
  if (!s || (s.min === null && s.max === null)) return null
  const k = (n: number) => {
    const thousands = n / 1000
    // whole k when clean, else one decimal (e.g. 92.5k)
    return Number.isInteger(thousands) ? `${thousands}k` : `${thousands.toFixed(1)}k`
  }
  const cur = s.currency && s.currency !== 'USD' ? ` ${s.currency}` : ''
  if (s.min !== null && s.max !== null) return `$${k(s.min)}–${k(s.max)}${cur}`
  if (s.min !== null) return `$${k(s.min)}+${cur}`
  return `up to $${k(s.max as number)}${cur}`
}

/** Sponsorship badge text + tone. UNKNOWN reads as a quiet em-dash, not a scary flag. */
export function sponsorshipBadge(signal: string): { label: string; tone: 'ok' | 'danger' | 'mute' } {
  switch (signal) {
    case 'OFFERED':
      return { label: 'sponsorship', tone: 'ok' }
    case 'NOT_OFFERED':
      return { label: 'no sponsorship', tone: 'danger' }
    default:
      return { label: 'sponsorship —', tone: 'mute' }
  }
}

/** Enum → human, sentence case. FULL_TIME → "full-time", NEW_GRAD → "new grad". */
export function humanize(enumValue: string): string {
  return enumValue.toLowerCase().replace(/_/g, ' ').replace('full time', 'full-time')
}

/** Short source tag for the badge. */
export function sourceTag(source: string | null): string | null {
  if (!source) return null
  const map: Record<string, string> = { GREENHOUSE: 'GH', LEVER: 'LV', ASHBY: 'AS' }
  return map[source] ?? source.slice(0, 2)
}

/** The one-line meta row: company · location · type, skipping unknowns. */
export function metaLine(p: PostingSummary): string {
  const parts: string[] = []
  if (p.companyName) parts.push(p.companyName)
  if (p.location) parts.push(p.location)
  if (p.remoteType && p.remoteType !== 'UNKNOWN') parts.push(humanize(p.remoteType))
  if (p.employmentType && p.employmentType !== 'UNKNOWN') parts.push(humanize(p.employmentType))
  return parts.join(' · ')
}
