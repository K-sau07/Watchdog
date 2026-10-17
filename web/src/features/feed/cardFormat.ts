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
  const sym = currencyPrefix(s.currency)
  if (s.min !== null && s.max !== null) return `${sym}${k(s.min)}–${k(s.max)}`
  if (s.min !== null) return `${sym}${k(s.min)}+`
  return `up to ${sym}${k(s.max as number)}`
}

/**
 * Currency marker for a salary figure.
 *
 * <p>A hardcoded "$" used to go in front of every number, with the real code appended
 * after — so a Canadian range read "$120k–150k CAD", and a euro range read as dollars
 * outright. An unknown currency gets no marker at all: the heuristic description parser
 * that produces those rows never saw a currency, and inventing one is the error that
 * actually costs something.
 */
function currencyPrefix(currency: string | null): string {
  switch (currency) {
    case 'USD':
      return '$'
    case 'CAD':
      return 'CA$'
    case 'AUD':
      return 'A$'
    case 'EUR':
      return '€'
    case 'GBP':
      return '£'
    case null:
    case '':
      return ''
    default:
      return `${currency} `
  }
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
