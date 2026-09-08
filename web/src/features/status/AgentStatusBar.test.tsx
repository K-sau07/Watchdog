import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { AgentStatusBar } from './AgentStatusBar'
import type { AgentStatus } from '../../lib/api'

const NOW = Date.parse('2026-09-08T12:00:00Z')

const status: AgentStatus = {
  boardsWatched: 25,
  greenhouse: 16,
  lever: 2,
  ashby: 7,
  lastPoll: '2026-09-08T11:59:22Z',
  nextPoll: '2026-09-08T12:01:22Z',
  newToday: 12,
  medianCatchMinutesToday: 6,
}

describe('AgentStatusBar', () => {
  it('shows boards watched, poll timing, and new-today when live', () => {
    render(<AgentStatusBar status={status} isError={false} nowMs={NOW} />)
    expect(screen.getByText(/watching · 25 boards/i)).toBeInTheDocument()
    expect(screen.getByText(/last poll 38s ago/i)).toBeInTheDocument()
    expect(screen.getByText(/next in 1:22/i)).toBeInTheDocument()
    expect(screen.getByText(/12 new today/i)).toBeInTheDocument()
  })

  it('degrades honestly when the agent is unreachable', () => {
    render(<AgentStatusBar status={undefined} isError nowMs={NOW} />)
    expect(screen.getByText(/agent unreachable/i)).toBeInTheDocument()
    expect(screen.queryByText(/boards/i)).not.toBeInTheDocument()
  })

  it('shows a connecting state before the first load', () => {
    render(<AgentStatusBar status={undefined} isError={false} nowMs={NOW} />)
    expect(screen.getByText(/connecting/i)).toBeInTheDocument()
  })
})
