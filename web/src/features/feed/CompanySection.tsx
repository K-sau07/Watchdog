import { useEffect, useRef, useState } from 'react'
import type { JobState } from '../../lib/api'
import type { CompanyGroup } from '../../lib/grouping'
import { JobCard } from './JobCard'

export interface CompanySectionProps {
  group: CompanyGroup
  onState: (id: string, state: JobState) => void
  freshIds?: ReadonlySet<string>
}

/**
 * A collapsible company section (D-WD15): one row per company so a company with many
 * openings doesn't flood the feed. Single-role companies render the card directly (no
 * expander needed); multi-role companies show a header with the count and expand to reveal
 * every role. Auto-expands when any of its roles is freshly-arrived, so new drops aren't
 * hidden behind a collapsed header.
 */
export function CompanySection({ group, onState, freshIds }: CompanySectionProps) {
  const hasFresh = freshIds ? group.roles.some((r) => freshIds.has(r.id)) : false
  // Auto-open once when a fresh role arrives, but stay fully user-controlled after —
  // hasFresh must NOT force the section open, or the close button can't override it.
  const [open, setOpen] = useState(hasFresh)
  const wasFresh = useRef(hasFresh)
  useEffect(() => {
    if (hasFresh && !wasFresh.current) setOpen(true) // newly fresh → pop open
    wasFresh.current = hasFresh
  }, [hasFresh])
  const expanded = open

  // Single role → just the card, no section chrome.
  if (group.roles.length === 1) {
    const only = group.roles[0]
    return <JobCard posting={only} onState={(s) => onState(only.id, s)} justArrived={freshIds?.has(only.id)} />
  }

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={expanded}
        className="flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-card-hover"
      >
        <span className="flex items-center gap-2">
          <span className="font-display text-[16px] font-medium text-text-hi">{group.company}</span>
          <span className="rounded-full bg-badge px-2 py-0.5 font-mono text-[11px] text-text-mid">
            {group.roles.length} roles
          </span>
          {hasFresh ? (
            <span className="rounded-full px-2 py-0.5 text-[11px]" style={{ background: 'var(--amber-glow)', color: 'var(--color-amber)' }}>
              new
            </span>
          ) : null}
        </span>
        <span aria-hidden="true" className="text-text-lo transition-transform" style={{ transform: expanded ? 'rotate(90deg)' : 'none' }}>
          ›
        </span>
      </button>

      {expanded ? (
        <div className="flex flex-col gap-2 px-2 pb-2">
          {group.roles.map((r) => (
            <JobCard key={r.id} posting={r} onState={(s) => onState(r.id, s)} justArrived={freshIds?.has(r.id)} />
          ))}
        </div>
      ) : null}
    </div>
  )
}
