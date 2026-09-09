import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { CompanySection } from './CompanySection'
import type { CompanyGroup } from '../../lib/grouping'
import type { PostingSummary } from '../../lib/api'

function role(id: string): PostingSummary {
  return {
    id, title: `Role ${id}`, companyName: 'Stripe', location: null, remoteType: 'REMOTE',
    employmentType: 'FULL_TIME', seniority: 'MID', sponsorshipSignal: 'UNKNOWN',
    salary: null, url: null, source: 'GREENHOUSE', postedAt: null,
    firstSeenAt: '2026-09-08T12:00:00Z', caughtMinutes: null,
  }
}

const group: CompanyGroup = { company: 'Stripe', source: 'GREENHOUSE', roles: [role('a'), role('b')] }

describe('CompanySection', () => {
  it('starts collapsed and toggles open/closed on click', () => {
    render(<CompanySection group={group} onState={() => {}} />)
    // collapsed: roles hidden
    expect(screen.queryByText('Role a')).not.toBeInTheDocument()
    const header = screen.getByRole('button', { name: /Stripe/i })

    fireEvent.click(header) // open
    expect(screen.getByText('Role a')).toBeInTheDocument()

    fireEvent.click(header) // close — this is the bug that regressed: must actually close
    expect(screen.queryByText('Role a')).not.toBeInTheDocument()
  })

  it('auto-opens when a role is fresh, but can still be closed', () => {
    const fresh = new Set(['a'])
    render(<CompanySection group={group} onState={() => {}} freshIds={fresh} />)
    // fresh → starts open
    expect(screen.getByText('Role a')).toBeInTheDocument()
    // user can still close it (hasFresh must not force it open)
    fireEvent.click(screen.getByRole('button', { name: /Stripe/i }))
    expect(screen.queryByText('Role a')).not.toBeInTheDocument()
  })
})
