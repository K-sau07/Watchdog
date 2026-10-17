import { describe, it, expect } from 'vitest'
import type { OutreachPerson } from '../../lib/api'
import {
  owes,
  owingLabel,
  channelChips,
  touchedEmail,
  touchedLinkedIn,
  coverageLabel,
  paceLabel,
  sinceLabel,
  STAGE_LABEL,
  TYPE_LABEL,
} from './outreachFormat'

const NOW = Date.parse('2026-10-10T12:00:00Z')

function person(over: Partial<OutreachPerson> = {}): OutreachPerson {
  return {
    id: 'c1',
    companyName: 'Ramp',
    personName: 'Dana Lee',
    personTitle: 'Engineering Manager',
    linkedinUrl: null,
    email: null,
    personType: 'HIRING_MANAGER',
    canGive: null,
    stage: 'REACHED_OUT',
    stageDeclared: false,
    turns: 0,
    unanswered: 1,
    channelsTouched: ['LINKEDIN_DM'],
    unansweredPerChannel: { LINKEDIN_DM: 1 },
    reachedOutAt: '2026-10-08T12:00:00Z',
    lastOutboundAt: '2026-10-08T12:00:00Z',
    lastInboundAt: null,
    followUpDueOn: null,
    postingId: null,
    ...over,
  }
}

describe('owes', () => {
  it('calls for a chase when the backend says one is due', () => {
    const p = person({ followUpDueOn: 'LINKEDIN_DM' })
    expect(owes(p)).toBe('chase')
    expect(owingLabel('chase', p)).toBe('follow up today')
  })

  it('names the channel when the chase should go by email', () => {
    const p = person({ followUpDueOn: 'EMAIL' })
    expect(owingLabel(owes(p), p)).toBe('follow up by email')
  })

  it('puts the ball with you when they spoke last', () => {
    const p = person({
      stage: 'LIVE',
      turns: 1,
      lastOutboundAt: '2026-10-08T12:00:00Z',
      lastInboundAt: '2026-10-09T12:00:00Z',
    })
    expect(owes(p)).toBe('you')
  })

  it('waits on them when we spoke last and nothing is due yet', () => {
    const p = person({
      stage: 'LIVE',
      turns: 1,
      lastInboundAt: '2026-10-08T12:00:00Z',
      lastOutboundAt: '2026-10-09T12:00:00Z',
    })
    expect(owes(p)).toBe('them')
  })

  it('stops once every channel is spent', () => {
    const p = person({ stage: 'NURTURE' })
    expect(owes(p)).toBe('stop')
    expect(owingLabel('stop')).toMatch(/comments only/i)
  })

  it('owes nobody on a won or dead thread', () => {
    expect(owes(person({ stage: 'WON' }))).toBe('none')
    expect(owes(person({ stage: 'DEAD', followUpDueOn: 'EMAIL' }))).toBe('none')
    expect(owingLabel('none')).toBeNull()
  })
})

describe('channels', () => {
  it('counts an invite as having touched linkedin', () => {
    // An invite spends no budget but it is still a door we have knocked on, and the
    // board's coverage count would be wrong if it said otherwise.
    const p = person({ channelsTouched: ['LINKEDIN_INVITE'] })
    expect(touchedLinkedIn(p)).toBe(true)
    expect(touchedEmail(p)).toBe(false)
    expect(channelChips(p)).toEqual(['in'])
  })

  it('shows both chips once both channels are used', () => {
    const p = person({ channelsTouched: ['LINKEDIN_INVITE', 'LINKEDIN_DM', 'EMAIL'] })
    // Invite and DM are one door, so "in" appears once rather than twice.
    expect(channelChips(p)).toEqual(['in', 'email'])
  })

  it('shows nothing for someone only drafted to', () => {
    expect(channelChips(person({ channelsTouched: [] }))).toEqual([])
  })
})

describe('labels', () => {
  it('reads the roster against the target', () => {
    expect(
      coverageLabel({
        target: 8,
        invited: 4,
        emailed: 3,
        awaitingReply: [1, 2, 3],
        inConversation: [4, 5],
      }),
    ).toBe('5 of 8 targeted · 4 on linkedin · 3 emailed')
  })

  it('reads the week against the cap', () => {
    expect(
      paceLabel({ invitesSent: 160, inviteCap: 200, companiesTouched: 18, companiesTarget: 20 }),
    ).toBe('160 / 200 invites · 18 of 20 companies')
  })

  it('says today rather than 0d ago', () => {
    expect(sinceLabel('2026-10-10T09:00:00Z', NOW)).toBe('today')
    expect(sinceLabel('2026-10-09T09:00:00Z', NOW)).toBe('yesterday')
    expect(sinceLabel('2026-10-07T09:00:00Z', NOW)).toBe('3d ago')
    expect(sinceLabel(null, NOW)).toBe('—')
  })

  it('labels every stage and every person type', () => {
    expect(Object.values(STAGE_LABEL).every(Boolean)).toBe(true)
    expect(Object.values(TYPE_LABEL).every(Boolean)).toBe(true)
    expect(STAGE_LABEL.REACHED_OUT).toBe('no reply yet')
  })
})
