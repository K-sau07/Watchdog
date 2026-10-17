import { describe, it, expect } from 'vitest'
import { formatSalary, sponsorshipBadge, humanize, sourceTag, metaLine } from './cardFormat'
import type { PostingSummary } from '../../lib/api'

describe('formatSalary', () => {
  it('formats a range with k-suffix', () => {
    expect(formatSalary({ min: 120000, max: 150000, currency: 'USD' })).toBe('$120k–150k')
  })
  it('formats min-only and max-only', () => {
    expect(formatSalary({ min: 120000, max: null, currency: 'USD' })).toBe('$120k+')
    expect(formatSalary({ min: null, max: 150000, currency: 'USD' })).toBe('up to $150k')
  })
  it('uses the right symbol per currency, never a dollar sign with a foreign code', () => {
    expect(formatSalary({ min: 110000, max: 145000, currency: 'CAD' })).toBe('CA$110k–145k')
    expect(formatSalary({ min: 60000, max: 120000, currency: 'EUR' })).toBe('€60k–120k')
    expect(formatSalary({ min: 70000, max: 90000, currency: 'GBP' })).toBe('£70k–90k')
    expect(formatSalary({ min: 70000, max: 90000, currency: 'SEK' })).toBe('SEK 70k–90k')
  })
  it('keeps one decimal for non-round k', () => {
    expect(formatSalary({ min: 92500, max: null, currency: 'USD' })).toBe('$92.5k+')
  })
  it('is null when empty', () => {
    expect(formatSalary(null)).toBeNull()
    expect(formatSalary({ min: null, max: null, currency: null })).toBeNull()
  })

  it('claims no currency when the parser never saw one', () => {
    // These rows come from the heuristic description parser, which reads numbers out of
    // prose and has no currency to report. A "$" here would be an invention.
    expect(formatSalary({ min: 3000, max: 200000, currency: null })).toBe('3k–200k')
    expect(formatSalary({ min: 120000, max: null, currency: null })).toBe('120k+')
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
    sponsorshipSignal: 'OFFERED', salary: null, url: null, source: 'GREENHOUSE', fundingStage: null,
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
