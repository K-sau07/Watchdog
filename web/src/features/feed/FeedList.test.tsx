import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { FeedList } from './FeedList'
import type { PostingSummary } from '../../lib/api'

const posting: PostingSummary = {
  id: 'p1', title: 'Software Engineer, New Grad', companyName: 'Ramp',
  location: 'NYC', remoteType: 'REMOTE', employmentType: 'FULL_TIME',
  seniority: 'NEW_GRAD', sponsorshipSignal: 'OFFERED', salary: null, url: null,
  source: 'GREENHOUSE', postedAt: null, firstSeenAt: '2026-09-08T12:00:00Z', caughtMinutes: 2,
}

const noop = () => {}

const base = {
  postings: [],
  isLoading: false,
  isError: false,
  hasActiveFilters: false,
  onState: noop,
  onResetFilters: noop,
  onRetry: noop,
}

describe('FeedList', () => {
  it('shows an error panel with retry', () => {
    const onRetry = vi.fn()
    render(<FeedList {...base} isError onRetry={onRetry} />)
    expect(screen.getByText(/couldn't reach the agent/i)).toBeInTheDocument()
    fireEvent.click(screen.getByText('retry'))
    expect(onRetry).toHaveBeenCalledOnce()
  })

  it('shows the scanning state while loading with no data', () => {
    render(<FeedList {...base} isLoading />)
    expect(screen.getByText(/scanning boards/i)).toBeInTheDocument()
  })

  it('invites action when empty with no filters', () => {
    render(<FeedList {...base} />)
    expect(screen.getByText(/no catches yet/i)).toBeInTheDocument()
  })

  it('offers reset when empty with active filters', () => {
    const onResetFilters = vi.fn()
    render(<FeedList {...base} hasActiveFilters onResetFilters={onResetFilters} />)
    expect(screen.getByText(/nothing matches these filters/i)).toBeInTheDocument()
    fireEvent.click(screen.getByText('reset filters'))
    expect(onResetFilters).toHaveBeenCalledOnce()
  })

  it('renders cards and routes state changes with the posting id', () => {
    const onState = vi.fn()
    render(<FeedList {...base} postings={[posting]} onState={onState} />)
    expect(screen.getByRole('heading', { name: /software engineer/i })).toBeInTheDocument()
    fireEvent.click(screen.getByText('save'))
    expect(onState).toHaveBeenCalledWith('p1', 'SAVED')
  })
})
