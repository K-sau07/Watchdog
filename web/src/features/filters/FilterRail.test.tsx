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
    const input = screen.getByLabelText('job title')
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

  it('renders its own heading by default', () => {
    render(<FilterRail value={EMPTY_FILTER} onChange={vi.fn()} onReset={vi.fn()} />)
    expect(screen.getByRole('heading', { name: 'filters' })).toBeInTheDocument()
  })

  it('drops the heading inside the drawer, which supplies its own', () => {
    const active = { ...EMPTY_FILTER, usOnly: true }
    render(
      <FilterRail value={active} onChange={vi.fn()} onReset={vi.fn()} showHeading={false} />,
    )
    expect(screen.queryByRole('heading', { name: 'filters' })).not.toBeInTheDocument()
    // the reset control belongs to the filter state, so it survives
    expect(screen.getByText(/reset \(1\)/)).toBeInTheDocument()
  })

  it('offers the company-stage and role-type dimensions', () => {
    render(<FilterRail value={EMPTY_FILTER} onChange={vi.fn()} onReset={vi.fn()} />)
    expect(screen.getByText('company stage')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'series B+' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'individual contributor' })).toBeInTheDocument()
  })
})
