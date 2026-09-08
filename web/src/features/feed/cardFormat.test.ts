import { describe, it, expect } from 'vitest'
import { formatSalary, sponsorshipBadge, humanize, sourceTag, metaLine } from './cardFormat'
import type { PostingSummary } from '../../lib/api'

describe('formatSalary', () => {
  it('formats a range with k-suffix', () => {
    expect(formatSalary({ min: 120000, max: 150000, currency: 'USD' })).toBe('$120k–150k')
  })
  it('formats min-only and max-only', () => {
    expect(formatSalary({ min: 120000, max: null, currency: null })).toBe('$120k+')
    expect(formatSalary({ min: null, max: 150000, currency: null })).toBe('up to $150k')
  })
  it('shows non-USD currency', () => {
    expect(formatSalary({ min: 110000, max: 145000, currency: 'CAD' })).toBe('$110k–145k CAD')
  })
  it('keeps one decimal for non-round k', () => {
    expect(formatSalary({ min: 92500, max: null, currency: null })).toBe('$92.5k+')
  })
  it('is null when empty', () => {
    expect(formatSalary(null)).toBeNull()
    expect(formatSalary({ min: null, max: null, currency: null })).toBeNull()
  })
})

describe('sponsorshipBadge', () => {
  it('maps each signal to label + tone', () => {
    expect(sponsorshipBadge('OFFERED')).toEqual({ label: 'sponsorship', tone: 'ok' })
    expect(sponsorshipBadge('NOT_OFFERED')).toEqual({ label: 'no sponsorship', tone: 'danger' })
    expect(sponsorshipBadge('UNKNOWN')).toEqual({ label: 'sponsorship —', tone: 'mute' })
  })
})

describe('humanize', () => {
  it('lowercases and de-underscores, fixing full-time', () => {
    expect(humanize('FULL_TIME')).toBe('full-time')
    expect(humanize('NEW_GRAD')).toBe('new grad')
    expect(humanize('INTERNSHIP')).toBe('internship')
  })
})

describe('sourceTag', () => {
  it('maps known sources and passes null through', () => {
    expect(sourceTag('GREENHOUSE')).toBe('GH')
    expect(sourceTag('LEVER')).toBe('LV')
    expect(sourceTag('ASHBY')).toBe('AS')
    expect(sourceTag(null)).toBeNull()
  })
})

describe('metaLine', () => {
  const base: PostingSummary = {
    id: '1', title: 'SWE', companyName: 'Ramp', location: 'New York',
    remoteType: 'REMOTE', employmentType: 'FULL_TIME', seniority: 'NEW_GRAD',
    sponsorshipSignal: 'OFFERED', salary: null, url: null, source: 'GREENHOUSE',
    postedAt: null, firstSeenAt: '2026-09-08T12:00:00Z', caughtMinutes: 2,
  }
  it('joins company, location, remote, type', () => {
    expect(metaLine(base)).toBe('Ramp · New York · remote · full-time')
  })
  it('skips unknowns and nulls', () => {
    expect(metaLine({ ...base, companyName: null, remoteType: 'UNKNOWN' })).toBe(
      'New York · full-time',
    )
  })
})
