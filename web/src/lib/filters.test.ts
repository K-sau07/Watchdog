import { describe, it, expect } from 'vitest'
import { EMPTY_FILTER, toQueryString, activeCount, type FilterState } from './filters'

describe('toQueryString', () => {
  it('is empty for the match-all filter', () => {
    expect(toQueryString(EMPTY_FILTER)).toBe('')
  })

  it('comma-joins list dimensions and omits empties', () => {
    const f: FilterState = {
      ...EMPTY_FILTER,
      roles: ['new grad', 'swe'],
      seniority: ['NEW_GRAD', 'JUNIOR'],
    }
    const qs = new URLSearchParams(toQueryString(f))
    expect(qs.get('roles')).toBe('new grad,swe')
    expect(qs.get('seniority')).toBe('NEW_GRAD,JUNIOR')
    expect(qs.has('keywords')).toBe(false)
  })

  it('serializes scalars and skips page 0 / null size', () => {
    const f: FilterState = {
      ...EMPTY_FILTER,
      salaryMin: 130000,
      includeUnknownSalary: false,
      sponsorship: 'HIDE_NOT_OFFERED',
      postedWithin: '1h',
    }
    const qs = new URLSearchParams(toQueryString(f))
    expect(qs.get('salaryMin')).toBe('130000')
    expect(qs.get('includeUnknownSalary')).toBe('false')
    expect(qs.get('sponsorship')).toBe('HIDE_NOT_OFFERED')
    expect(qs.get('postedWithin')).toBe('1h')
    expect(qs.has('page')).toBe(false)
    expect(qs.has('size')).toBe(false)
  })

  it('includes page when > 0', () => {
    const qs = new URLSearchParams(toQueryString({ ...EMPTY_FILTER, page: 2 }))
    expect(qs.get('page')).toBe('2')
  })

  it('serializes usOnly only when true', () => {
    expect(new URLSearchParams(toQueryString(EMPTY_FILTER)).has('usOnly')).toBe(false)
    expect(new URLSearchParams(toQueryString({ ...EMPTY_FILTER, usOnly: true })).get('usOnly')).toBe('true')
  })
})

describe('activeCount', () => {
  it('is zero for the empty filter', () => {
    expect(activeCount(EMPTY_FILTER)).toBe(0)
  })

  it('counts each constraining dimension once; date range counts as one', () => {
    const f: FilterState = {
      ...EMPTY_FILTER,
      roles: ['swe'],
      salaryMin: 100000,
      dateFrom: '2026-09-01T00:00:00Z',
      dateTo: '2026-09-08T00:00:00Z',
    }
    expect(activeCount(f)).toBe(3) // roles + salaryMin + (date range = 1)
  })
})
