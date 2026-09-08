import { describe, it, expect } from 'vitest'
import {
  temperatureOf,
  catchLabel,
  isFresh,
  FRESH_HOT_MAX,
  FRESH_STEADY_MAX,
  FRESH_DIM_MAX,
} from './freshness'

describe('temperatureOf', () => {
  it('buckets by catch-minutes at the ramp boundaries', () => {
    expect(temperatureOf(0)).toBe('hot')
    expect(temperatureOf(FRESH_HOT_MAX - 1)).toBe('hot') // 1 min
    expect(temperatureOf(FRESH_HOT_MAX)).toBe('steady') // 2 min → steady
    expect(temperatureOf(FRESH_STEADY_MAX - 1)).toBe('steady') // 14 min
    expect(temperatureOf(FRESH_STEADY_MAX)).toBe('dim') // 15 min → dim
    expect(temperatureOf(FRESH_DIM_MAX - 1)).toBe('dim') // 59 min
    expect(temperatureOf(FRESH_DIM_MAX)).toBe('cooled') // 60 min → cooled
    expect(temperatureOf(9999)).toBe('cooled')
  })

  it('is unknown when catch-time is unknown (never faked)', () => {
    expect(temperatureOf(null)).toBe('unknown')
  })
})

describe('catchLabel', () => {
  it('formats minutes', () => {
    expect(catchLabel(0)).toBe('caught <1 min')
    expect(catchLabel(2)).toBe('caught 2 min')
    expect(catchLabel(59)).toBe('caught 59 min')
  })

  it('formats hours', () => {
    expect(catchLabel(60)).toBe('caught 1 hr')
    expect(catchLabel(185)).toBe('caught 3 hr')
  })

  it('formats days with pluralization', () => {
    expect(catchLabel(60 * 24)).toBe('caught 1 day')
    expect(catchLabel(60 * 24 * 3)).toBe('caught 3 days')
  })

  it('shows an honest em-dash when unknown', () => {
    expect(catchLabel(null)).toBe('caught —')
  })
})

describe('isFresh', () => {
  it('is true only for the hottest bucket', () => {
    expect(isFresh(0)).toBe(true)
    expect(isFresh(1)).toBe(true)
    expect(isFresh(2)).toBe(false)
    expect(isFresh(null)).toBe(false)
  })
})
