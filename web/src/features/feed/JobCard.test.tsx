import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { JobCard } from './JobCard'
import type { PostingSummary } from '../../lib/api'

const posting: PostingSummary = {
  id: 'p1',
  title: 'Software Engineer, New Grad',
  companyName: 'Ramp',
  location: 'New York, NY',
  remoteType: 'ONSITE',
  employmentType: 'FULL_TIME',
  seniority: 'NEW_GRAD',
  sponsorshipSignal: 'OFFERED',
  salary: { min: 120000, max: 150000, currency: 'USD' },
  url: 'https://example.com/job',
  source: 'GREENHOUSE',
  postedAt: '2026-09-08T11:56:00Z',
  firstSeenAt: '2026-09-08T12:00:00Z',
  caughtMinutes: 4,
}

describe('JobCard', () => {
  it('renders title, company meta, salary, and the catch stamp', () => {
    render(<JobCard posting={posting} />)
    expect(screen.getByRole('heading', { name: /software engineer, new grad/i })).toBeInTheDocument()
    expect(screen.getByText(/Ramp · New York, NY · onsite · full-time/)).toBeInTheDocument()
    expect(screen.getByText('$120k–150k')).toBeInTheDocument()
    expect(screen.getByText('caught 4 min')).toBeInTheDocument()
  })

  it('links the title to the posting url', () => {
    render(<JobCard posting={posting} />)
    expect(screen.getByRole('link', { name: /software engineer/i })).toHaveAttribute(
      'href',
      'https://example.com/job',
    )
  })

  it('fires onState for save / applied / hide', () => {
    const onState = vi.fn()
    render(<JobCard posting={posting} onState={onState} />)
    fireEvent.click(screen.getByText('save'))
    fireEvent.click(screen.getByText('applied'))
    fireEvent.click(screen.getByLabelText(/hide this posting/i))
    expect(onState).toHaveBeenNthCalledWith(1, 'SAVED')
    expect(onState).toHaveBeenNthCalledWith(2, 'APPLIED')
    expect(onState).toHaveBeenNthCalledWith(3, 'HIDDEN')
  })

  it('shows an honest em-dash stamp when catch-time is unknown', () => {
    render(<JobCard posting={{ ...posting, caughtMinutes: null }} />)
    expect(screen.getByText('caught —')).toBeInTheDocument()
  })
})
