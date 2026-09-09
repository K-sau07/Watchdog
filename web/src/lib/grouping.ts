import type { PostingSummary } from './api'

/**
 * Group a flat, newest-posted-first list of postings into per-company sections, so one
 * company with many roles becomes a single expandable row instead of flooding the feed
 * (D-WD15 / D-WD16, grouped client-side). Pure and framework-free — unit-tested.
 *
 * Company order is preserved from the input: because the feed arrives sorted by postedAt
 * (newest first), the first company encountered is the one with the most-recent role, so
 * the section order is "company with the freshest posting first". Roles within a section
 * keep their incoming (newest-first) order.
 */
export interface CompanyGroup {
  /** Company display name, or "Unknown" if the posting had none. */
  company: string
  /** ATS source of the group (from the first role); may be null. */
  source: string | null
  roles: PostingSummary[]
}

export function groupByCompany(postings: PostingSummary[]): CompanyGroup[] {
  const order: string[] = []
  const byCompany = new Map<string, CompanyGroup>()

  for (const p of postings) {
    const key = p.companyName ?? 'Unknown'
    let group = byCompany.get(key)
    if (!group) {
      group = { company: key, source: p.source, roles: [] }
      byCompany.set(key, group)
      order.push(key)
    }
    group.roles.push(p)
  }

  return order.map((key) => byCompany.get(key)!)
}
