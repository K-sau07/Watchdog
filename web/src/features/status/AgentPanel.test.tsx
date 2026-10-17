import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AgentPanel } from './AgentPanel'
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
  caughtToday: 9,
}

const baseProps = {
  nowMs: NOW,
  onRefresh: () => {},
  isRefreshing: false,
  cooldownSeconds: null,
}

describe('AgentPanel', () => {
  it('shows boards watched, poll timing, and new-today when live', () => {
    render(<AgentPanel {...baseProps} status={status} isError={false} />)
    expect(screen.getByText(/watching · 25 boards/i)).toBeInTheDocument()
    // The readouts are a definition list now, so each term and its value are separate
    // nodes rather than one run-on string.
    expect(screen.getByText('last poll')).toBeInTheDocument()
    expect(screen.getByText(/38s ago/i)).toBeInTheDocument()
    expect(screen.getByText('next poll')).toBeInTheDocument()
    expect(screen.getByText(/1:22/)).toBeInTheDocument()
    expect(screen.getByText('new today')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
  })

  it('degrades honestly when the agent is unreachable', () => {
    render(<AgentPanel {...baseProps} status={undefined} isError />)
    expect(screen.getByText(/agent unreachable/i)).toBeInTheDocument()
    expect(screen.queryByText(/boards/i)).not.toBeInTheDocument()
  })

  it('shows a connecting state before the first load', () => {
    render(<AgentPanel {...baseProps} status={undefined} isError={false} />)
    expect(screen.getByText(/connecting/i)).toBeInTheDocument()
  })

  it('fires refresh and shows cooldown/refreshing states', () => {
    const onRefresh = vi.fn()
    const { rerender } = render(
      <AgentPanel {...baseProps} status={status} isError={false} onRefresh={onRefresh} />,
    )
    fireEvent.click(screen.getByText(/↻ refresh/i))
    expect(onRefresh).toHaveBeenCalledOnce()

    rerender(<AgentPanel {...baseProps} status={status} isError={false} isRefreshing />)
    expect(screen.getByText(/refreshing/i)).toBeInTheDocument()

    rerender(<AgentPanel {...baseProps} status={status} isError={false} cooldownSeconds={45} />)
    expect(screen.getByText(/wait 45s/i)).toBeInTheDocument()
  })
})
