import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { FilterRail } from './FilterRail'
import { EMPTY_FILTER } from '../../lib/filters'

describe('FilterRail', () => {
  it('toggles a seniority chip and resets page to 0', () => {
    const onChange = vi.fn()
    render(<FilterRail value={{ ...EMPTY_FILTER, page: 3 }} onChange={onChange} onReset={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'new grad' }))
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ seniority: ['NEW_GRAD'], page: 0 }),
    )
  })

  it('adds a role tag on Enter', () => {
    const onChange = vi.fn()
    render(<FilterRail value={EMPTY_FILTER} onChange={onChange} onReset={() => {}} />)
    const input = screen.getByLabelText('role')
    fireEvent.change(input, { target: { value: 'new grad' } })
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ roles: ['new grad'] }))
  })

  it('selects a single-choice posted-within token', () => {
    const onChange = vi.fn()
    render(<FilterRail value={EMPTY_FILTER} onChange={onChange} onReset={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: '1 hour' }))
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ postedWithin: '1h' }))
  })

  it('shows the active count and fires reset', () => {
    const onReset = vi.fn()
    render(
      <FilterRail
        value={{ ...EMPTY_FILTER, roles: ['swe'], salaryMin: 100000 }}
        onChange={() => {}}
        onReset={onReset}
      />,
    )
    const reset = screen.getByText(/reset \(2\)/)
    fireEvent.click(reset)
    expect(onReset).toHaveBeenCalledOnce()
  })

  it('has no reset control when nothing is active', () => {
    render(<FilterRail value={EMPTY_FILTER} onChange={() => {}} onReset={() => {}} />)
    expect(screen.queryByText(/reset/)).not.toBeInTheDocument()
  })
})
