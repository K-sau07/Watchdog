import { describe, it, expect } from 'vitest'
import { groupByCompany } from './grouping'
import type { PostingSummary } from './api'

function p(id: string, companyName: string | null, source = 'GREENHOUSE'): PostingSummary {
  return {
    id, title: `Role ${id}`, companyName, location: null, remoteType: 'REMOTE',
    employmentType: 'FULL_TIME', seniority: 'MID', sponsorshipSignal: 'UNKNOWN',
    salary: null, url: null, source, postedAt: null, firstSeenAt: '2026-09-08T12:00:00Z',
    caughtMinutes: null,
  }
}

describe('groupByCompany', () => {
  it('groups roles under their company', () => {
    const groups = groupByCompany([p('1', 'Stripe'), p('2', 'Stripe'), p('3', 'Linear')])
    expect(groups).toHaveLength(2)
    expect(groups[0].company).toBe('Stripe')
    expect(groups[0].roles.map((r) => r.id)).toEqual(['1', '2'])
    expect(groups[1].company).toBe('Linear')
  })

  it('preserves incoming company order (freshest-posting company first)', () => {
    // Feed arrives newest-first; Linear's role came before Stripe's here.
    const groups = groupByCompany([p('1', 'Linear'), p('2', 'Stripe'), p('3', 'Linear')])
    expect(groups.map((g) => g.company)).toEqual(['Linear', 'Stripe'])
    // Linear keeps both its roles, in order, even though Stripe appeared between them.
    expect(groups[0].roles.map((r) => r.id)).toEqual(['1', '3'])
  })

  it('buckets missing company names under "Unknown"', () => {
    const groups = groupByCompany([p('1', null), p('2', null)])
    expect(groups).toHaveLength(1)
    expect(groups[0].company).toBe('Unknown')
    expect(groups[0].roles).toHaveLength(2)
  })

  it('carries the source from the first role', () => {
    const groups = groupByCompany([p('1', 'Ramp', 'ASHBY')])
    expect(groups[0].source).toBe('ASHBY')
  })

  it('is empty for no postings', () => {
    expect(groupByCompany([])).toEqual([])
  })
})
