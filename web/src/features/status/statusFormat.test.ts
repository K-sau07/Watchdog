import { describe, it, expect } from 'vitest'
import { agoLabel, countdownLabel, medianLabel } from './statusFormat'

const NOW = Date.parse('2026-09-08T12:00:00Z')

describe('agoLabel', () => {
  it('formats seconds, minutes, hours', () => {
    expect(agoLabel('2026-09-08T11:59:22Z', NOW)).toBe('38s ago')
    expect(agoLabel('2026-09-08T11:58:00Z', NOW)).toBe('2m ago')
    expect(agoLabel('2026-09-08T10:00:00Z', NOW)).toBe('2h ago')
  })
  it('clamps future to 0s and handles null', () => {
    expect(agoLabel('2026-09-08T12:00:30Z', NOW)).toBe('0s ago')
    expect(agoLabel(null, NOW)).toBe('—')
  })
})

describe('countdownLabel', () => {
  it('formats m:ss remaining', () => {
    expect(countdownLabel('2026-09-08T12:01:22Z', NOW)).toBe('in 1:22')
    expect(countdownLabel('2026-09-08T12:00:05Z', NOW)).toBe('in 0:05')
  })
  it('says now when due/overdue, — when null', () => {
    expect(countdownLabel('2026-09-08T12:00:00Z', NOW)).toBe('now')
    expect(countdownLabel('2026-09-08T11:59:00Z', NOW)).toBe('now')
    expect(countdownLabel(null, NOW)).toBe('—')
  })
})

describe('medianLabel', () => {
  it('formats minutes and hours, honest when unknown', () => {
    expect(medianLabel(6)).toBe('median catch time today · 6 min')
    expect(medianLabel(0)).toBe('median catch time today · <1 min')
    expect(medianLabel(75)).toBe('median catch time today · 1 hr')
    expect(medianLabel(null)).toBe('no catches timed yet')
  })
})
