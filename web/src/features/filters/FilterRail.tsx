import { useState } from 'react'
import { type FilterState, activeCount } from '../../lib/filters'

/** Enum option sets for the multi-select groups (values match backend enum names). */
const SENIORITY = ['INTERN', 'NEW_GRAD', 'JUNIOR', 'MID', 'SENIOR'] as const
const REMOTE = ['REMOTE', 'HYBRID', 'ONSITE'] as const
const EMPLOYMENT = ['FULL_TIME', 'INTERNSHIP', 'CONTRACT', 'PART_TIME'] as const
const SOURCE = ['GREENHOUSE', 'LEVER', 'ASHBY'] as const
const STATE = ['NEW', 'SAVED', 'APPLIED', 'HIDDEN'] as const
const POSTED_WITHIN = [
  { token: '10m', label: '10 min' },
  { token: '30m', label: '30 min' },
  { token: '1h', label: '1 hour' },
  { token: 'today', label: 'today' },
  { token: 'week', label: 'this week' },
] as const
const SPONSORSHIP = [
  { value: 'REQUIRE_OFFERED', label: 'offered only' },
  { value: 'HIDE_NOT_OFFERED', label: 'hide no-sponsorship' },
] as const

function human(v: string): string {
  return v.toLowerCase().replace(/_/g, ' ').replace('full time', 'full-time')
}

export interface FilterRailProps {
  value: FilterState
  onChange: (next: FilterState) => void
  onReset: () => void
}

export function FilterRail({ value, onChange, onReset }: FilterRailProps) {
  const count = activeCount(value)
  const set = <K extends keyof FilterState>(key: K, v: FilterState[K]) =>
    onChange({ ...value, [key]: v, page: 0 })

  const toggle = (key: keyof FilterState, option: string) => {
    const arr = value[key] as string[]
    const next = arr.includes(option) ? arr.filter((x) => x !== option) : [...arr, option]
    set(key, next as FilterState[typeof key])
  }

  return (
    <aside className="flex w-full flex-col gap-5 border-r border-line bg-surface px-4 py-5 md:w-[280px] md:shrink-0">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-[15px] font-medium text-text-hi">filters</h2>
        {count > 0 ? (
          <button
            type="button"
            onClick={onReset}
            className="text-[12px] text-text-lo transition-colors hover:text-amber"
          >
            reset ({count})
          </button>
        ) : null}
      </div>

      <TagInput label="role" placeholder="new grad, swe…" tags={value.roles} onChange={(t) => set('roles', t)} />
      <TagInput label="include keywords" placeholder="python, remote…" tags={value.keywords} onChange={(t) => set('keywords', t)} />
      <TagInput label="exclude keywords" placeholder="senior, clearance…" tags={value.exclude} onChange={(t) => set('exclude', t)} />
      <TagInput label="location" placeholder="new york, remote…" tags={value.location} onChange={(t) => set('location', t)} />

      <div>
        <FieldLabel>country</FieldLabel>
        <Chip
          label="United States only"
          active={value.usOnly}
          onClick={() => set('usOnly', !value.usOnly)}
        />
      </div>

      <ChipGroup label="seniority" options={SENIORITY.map((v) => ({ value: v, label: human(v) }))} selected={value.seniority} onToggle={(v) => toggle('seniority', v)} />
      <ChipGroup label="remote" options={REMOTE.map((v) => ({ value: v, label: human(v) }))} selected={value.remote} onToggle={(v) => toggle('remote', v)} />
      <ChipGroup label="type" options={EMPLOYMENT.map((v) => ({ value: v, label: human(v) }))} selected={value.employmentType} onToggle={(v) => toggle('employmentType', v)} />

      <SingleChipGroup label="posted within" options={POSTED_WITHIN.map((o) => ({ value: o.token, label: o.label }))} selected={value.postedWithin} onSelect={(v) => set('postedWithin', v)} />
      <SingleChipGroup label="sponsorship" options={SPONSORSHIP.map((o) => ({ value: o.value, label: o.label }))} selected={value.sponsorship} onSelect={(v) => set('sponsorship', v)} />

      <NumberInput label="min salary" value={value.salaryMin} onChange={(n) => set('salaryMin', n)} />

      <ChipGroup label="source" options={SOURCE.map((v) => ({ value: v, label: human(v) }))} selected={value.source} onToggle={(v) => toggle('source', v)} />
      <ChipGroup label="state" options={STATE.map((v) => ({ value: v, label: human(v) }))} selected={value.state} onToggle={(v) => toggle('state', v)} />
    </aside>
  )
}

// --- controls ---

function FieldLabel({ children }: { children: React.ReactNode }) {
  return <span className="mb-1.5 block text-[12px] font-medium text-text-mid">{children}</span>
}

function Chip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className="rounded-full border px-2.5 py-1 text-[12px] transition-colors"
      style={{
        borderColor: active ? 'var(--color-amber-dim)' : 'var(--color-line)',
        color: active ? 'var(--color-amber)' : 'var(--color-text-mid)',
        background: active ? 'rgba(184,122,22,0.12)' : 'transparent',
      }}
    >
      {label}
    </button>
  )
}

function ChipGroup({
  label,
  options,
  selected,
  onToggle,
}: {
  label: string
  options: { value: string; label: string }[]
  selected: string[]
  onToggle: (value: string) => void
}) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <div className="flex flex-wrap gap-1.5">
        {options.map((o) => (
          <Chip key={o.value} label={o.label} active={selected.includes(o.value)} onClick={() => onToggle(o.value)} />
        ))}
      </div>
    </div>
  )
}

function SingleChipGroup({
  label,
  options,
  selected,
  onSelect,
}: {
  label: string
  options: { value: string; label: string }[]
  selected: string | null
  onSelect: (value: string | null) => void
}) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <div className="flex flex-wrap gap-1.5">
        {options.map((o) => (
          <Chip
            key={o.value}
            label={o.label}
            active={selected === o.value}
            onClick={() => onSelect(selected === o.value ? null : o.value)}
          />
        ))}
      </div>
    </div>
  )
}

function TagInput({
  label,
  placeholder,
  tags,
  onChange,
}: {
  label: string
  placeholder: string
  tags: string[]
  onChange: (tags: string[]) => void
}) {
  const [draft, setDraft] = useState('')
  const commit = () => {
    const t = draft.trim()
    if (t && !tags.includes(t)) onChange([...tags, t])
    setDraft('')
  }
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      {tags.length > 0 ? (
        <div className="mb-1.5 flex flex-wrap gap-1.5">
          {tags.map((t) => (
            <button
              key={t}
              type="button"
              aria-label={`Remove ${t}`}
              onClick={() => onChange(tags.filter((x) => x !== t))}
              className="rounded-full border border-amber-dim bg-[rgba(184,122,22,0.12)] px-2.5 py-1 text-[12px] text-amber"
            >
              {t} ✕
            </button>
          ))}
        </div>
      ) : null}
      <input
        type="text"
        value={draft}
        placeholder={placeholder}
        aria-label={label}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            commit()
          }
        }}
        onBlur={commit}
        className="w-full rounded border border-line bg-card px-2.5 py-1.5 text-[13px] text-text-hi placeholder:text-text-lo focus:border-amber-dim focus:outline-none"
      />
    </div>
  )
}

function NumberInput({
  label,
  value,
  onChange,
}: {
  label: string
  value: number | null
  onChange: (n: number | null) => void
}) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <input
        type="number"
        inputMode="numeric"
        value={value ?? ''}
        placeholder="120000"
        aria-label={label}
        onChange={(e) => {
          const n = e.target.value === '' ? null : Number(e.target.value)
          onChange(n !== null && Number.isFinite(n) ? n : null)
        }}
        className="w-full rounded border border-line bg-card px-2.5 py-1.5 font-mono text-[13px] text-text-hi placeholder:text-text-lo focus:border-amber-dim focus:outline-none"
      />
    </div>
  )
}
