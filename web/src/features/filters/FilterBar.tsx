import { useEffect, useRef, useState } from 'react'
import { type FilterState, activeCount } from '../../lib/filters'
import { CaretIcon } from '../../shell/navIcons'
import { FilterRail } from './FilterRail'

/**
 * The filter rail, rebuilt as a horizontal pill bar.
 *
 * The rail version put seventeen dimensions on screen at once and ate 280px of width
 * permanently. Every dimension is still here — they're behind pills now, and the pill
 * itself carries its own state ("series A +1"), so the bar reads as a sentence describing
 * the current feed rather than as a form you have to scan.
 */

const STAGE = [
  { value: 'SEED', label: 'seed' },
  { value: 'SERIES_A', label: 'series A' },
  { value: 'SERIES_B_PLUS', label: 'series B+' },
  { value: 'GROWTH', label: 'late stage' },
  { value: 'UNKNOWN', label: 'unverified' },
]
const SENIORITY = [
  { value: 'INTERN', label: 'intern' },
  { value: 'NEW_GRAD', label: 'new grad' },
  { value: 'JUNIOR', label: 'junior' },
  { value: 'MID', label: 'mid' },
  { value: 'SENIOR', label: 'senior' },
]
const EMPLOYMENT = [
  { value: 'FULL_TIME', label: 'full-time' },
  { value: 'INTERNSHIP', label: 'internship' },
  { value: 'CONTRACT', label: 'contract' },
  { value: 'PART_TIME', label: 'part-time' },
]
const REMOTE = [
  { value: 'REMOTE', label: 'remote' },
  { value: 'HYBRID', label: 'hybrid' },
  { value: 'ONSITE', label: 'onsite' },
]
const SOURCE = [
  { value: 'GREENHOUSE', label: 'Greenhouse' },
  { value: 'LEVER', label: 'Lever' },
  { value: 'ASHBY', label: 'Ashby' },
]
const POSTED_WITHIN = [
  { value: '10m', label: '10 min' },
  { value: '30m', label: '30 min' },
  { value: '1h', label: '1 hour' },
  { value: 'today', label: '24 hours' },
  { value: '3d', label: '3 days' },
  { value: 'week', label: 'this week' },
  { value: 'month', label: 'this month' },
]
const SPONSORSHIP = [
  { value: 'REQUIRE_OFFERED', label: 'sponsorship offered' },
  { value: 'HIDE_NOT_OFFERED', label: 'hide no-sponsorship' },
]
const ROLE_TYPE = [
  { value: 'IC', label: 'individual contributor' },
  { value: 'MANAGER', label: 'manager' },
]

export interface FilterBarProps {
  value: FilterState
  onChange: (next: FilterState) => void
  onReset: () => void
}

export function FilterBar({ value, onChange, onReset }: FilterBarProps) {
  const set = <K extends keyof FilterState>(key: K, v: FilterState[K]) =>
    onChange({ ...value, [key]: v, page: 0 })

  const n = activeCount(value)

  return (
    <div className="mb-5 flex flex-wrap items-center gap-2">
      <MultiPill
        name="Funding stage"
        options={STAGE}
        selected={value.stages}
        onChange={(v) => set('stages', v)}
      />
      <MultiPill
        name="Level"
        options={SENIORITY}
        selected={value.seniority}
        onChange={(v) => set('seniority', v)}
      />
      <MultiPill
        name="Type"
        options={EMPLOYMENT}
        selected={value.employmentType}
        onChange={(v) => set('employmentType', v)}
      />
      <MultiPill
        name="Workplace"
        options={REMOTE}
        selected={value.remote}
        onChange={(v) => set('remote', v)}
      />
      <SinglePill
        name="Posted"
        options={POSTED_WITHIN}
        selected={value.postedWithin}
        onChange={(v) => set('postedWithin', v)}
      />
      <SinglePill
        name="Sponsorship"
        options={SPONSORSHIP}
        selected={value.sponsorship}
        onChange={(v) => set('sponsorship', v)}
      />
      <SinglePill
        name="Role"
        options={ROLE_TYPE}
        selected={value.roleType}
        onChange={(v) => set('roleType', v)}
      />
      <MultiPill name="Board" options={SOURCE} selected={value.source} onChange={(v) => set('source', v)} />

      <TogglePill
        label="US only"
        active={value.usOnly}
        onClick={() => set('usOnly', !value.usOnly)}
      />

      {/* The dimensions that can't be a pill — free-text roles, keywords, exclusions,
          locations, a salary floor. The rail used to hold a permanent 280px column for
          these; behind a button it costs nothing until it's wanted. */}
      <AllFilters value={value} onChange={onChange} onReset={onReset} />

      {n > 0 ? (
        <button
          type="button"
          onClick={onReset}
          className="ml-1 rounded-pill px-3 py-1.5 text-[13px] text-text-lo underline-offset-2 transition-colors hover:text-text-hi hover:underline"
        >
          clear {n}
        </button>
      ) : null}
    </div>
  )
}

/** Pill shell: label, the current selection folded into it, and a caret. */
function Pill({
  summary,
  active,
  open,
  onToggle,
  children,
}: {
  summary: string
  active: boolean
  open: boolean
  onToggle: () => void
  children: React.ReactNode
}) {
  const wrap = useRef<HTMLDivElement>(null)

  // Close on outside click and on Escape. Both, because a dropdown that only closes by
  // clicking its own trigger again traps keyboard users.
  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (wrap.current && !wrap.current.contains(e.target as Node)) onToggle()
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onToggle()
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, onToggle])

  return (
    <div ref={wrap} className="relative">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        className={[
          'flex items-center gap-1.5 rounded-pill border px-3 py-1.5 text-[13px] transition-colors',
          active
            ? 'border-mint-edge bg-mint-soft font-medium text-text-hi'
            : 'border-line-strong bg-surface text-text-mid hover:bg-hover hover:text-text-hi',
        ].join(' ')}
      >
        {summary}
        <CaretIcon />
      </button>

      {open ? (
        <div className="absolute left-0 top-full z-30 mt-1.5 min-w-[200px] rounded-card border border-line-strong bg-surface p-1.5 shadow-[var(--shadow-raised)]">
          {children}
        </div>
      ) : null}
    </div>
  )
}

interface Option {
  value: string
  label: string
}

/** "Level" → "mid +1" once something is picked, so the bar states the current feed. */
function summarize(name: string, labels: string[]): string {
  if (labels.length === 0) return name
  if (labels.length === 1) return labels[0]
  return `${labels[0]} +${labels.length - 1}`
}

function MultiPill({
  name,
  options,
  selected,
  onChange,
}: {
  name: string
  options: Option[]
  selected: string[]
  onChange: (next: string[]) => void
}) {
  const [open, setOpen] = useState(false)
  const labels = options.filter((o) => selected.includes(o.value)).map((o) => o.label)

  const toggle = (v: string) =>
    onChange(selected.includes(v) ? selected.filter((x) => x !== v) : [...selected, v])

  return (
    <Pill
      summary={summarize(name, labels)}
      active={selected.length > 0}
      open={open}
      onToggle={() => setOpen((o) => !o)}
    >
      <fieldset>
        <legend className="px-2 py-1 text-[11px] uppercase tracking-wide text-text-faint">{name}</legend>
        {options.map((o) => (
          <label key={o.value} className={row}>
            <input
              type="checkbox"
              checked={selected.includes(o.value)}
              onChange={() => toggle(o.value)}
              className="accent-mint"
            />
            {o.label}
          </label>
        ))}
      </fieldset>
    </Pill>
  )
}

function SinglePill({
  name,
  options,
  selected,
  onChange,
}: {
  name: string
  options: Option[]
  selected: string | null
  onChange: (next: string | null) => void
}) {
  const [open, setOpen] = useState(false)
  const hit = options.find((o) => o.value === selected)

  return (
    <Pill
      summary={hit ? hit.label : name}
      active={selected !== null}
      open={open}
      onToggle={() => setOpen((o) => !o)}
    >
      <fieldset>
        <legend className="px-2 py-1 text-[11px] uppercase tracking-wide text-text-faint">{name}</legend>
        <label className={row}>
          <input
            type="radio"
            name={name}
            checked={selected === null}
            onChange={() => {
              onChange(null)
              setOpen(false)
            }}
            className="accent-mint"
          />
          any
        </label>
        {options.map((o) => (
          <label key={o.value} className={row}>
            <input
              type="radio"
              name={name}
              checked={selected === o.value}
              onChange={() => {
                onChange(o.value)
                setOpen(false)
              }}
              className="accent-mint"
            />
            {o.label}
          </label>
        ))}
      </fieldset>
    </Pill>
  )
}

function TogglePill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={[
        'rounded-pill border px-3 py-1.5 text-[13px] transition-colors',
        active
          ? 'border-mint-edge bg-mint-soft font-medium text-text-hi'
          : 'border-line-strong bg-surface text-text-mid hover:bg-hover hover:text-text-hi',
      ].join(' ')}
    >
      {label}
    </button>
  )
}

const row =
  'flex cursor-pointer items-center gap-2.5 rounded-control px-2 py-1.5 text-[13px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi'

/** The full rail, as a slide-over. */
function AllFilters({ value, onChange, onReset }: FilterBarProps) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="rounded-pill border border-line-strong bg-surface px-3 py-1.5 text-[13px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi"
      >
        ••• All filters
      </button>

      {open ? (
        <div className="fixed inset-0 z-40">
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setOpen(false)}
            aria-hidden="true"
          />
          <div
            role="dialog"
            aria-label="All filters"
            className="absolute inset-y-0 right-0 w-[88%] max-w-[360px] overflow-y-auto bg-surface shadow-[var(--shadow-raised)]"
          >
            <div className="sticky top-0 flex items-center justify-between border-b border-line bg-surface px-4 py-3">
              <span className="text-[15px] font-semibold text-text-hi">All filters</span>
              <button
                type="button"
                aria-label="Close filters"
                onClick={() => setOpen(false)}
                className="rounded-control px-2 py-1 text-text-mid transition-colors hover:bg-hover hover:text-text-hi"
              >
                ✕
              </button>
            </div>
            <FilterRail value={value} onChange={onChange} onReset={onReset} showHeading={false} />
          </div>
        </div>
      ) : null}
    </>
  )
}
