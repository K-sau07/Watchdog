import type { PostingSummary } from '../../lib/api'
import type { JobState } from '../../lib/api'
import { catchLabel, temperatureOf, type Temperature } from '../../lib/freshness'
import { formatSalary, sponsorshipBadge, humanize, sourceTag, metaLine } from './cardFormat'

/** Amber temperature → the colors that express freshness (bible §2 ramp). */
function rampColor(t: Temperature): { edge: string; stamp: string; glow: string; dot: string } {
  switch (t) {
    case 'hot':
      return { edge: 'var(--color-amber-hot)', stamp: 'var(--color-amber-hot)', glow: 'var(--amber-glow-hot)', dot: 'var(--color-amber-hot)' }
    case 'steady':
      return { edge: 'var(--color-amber)', stamp: 'var(--color-amber)', glow: 'var(--amber-glow)', dot: 'var(--color-amber)' }
    case 'dim':
      return { edge: 'var(--color-amber-dim)', stamp: 'var(--color-amber-dim)', glow: 'transparent', dot: 'var(--color-amber-dim)' }
    default: // cooled / unknown → no amber, fully neutral
      return { edge: 'var(--color-line-strong)', stamp: 'var(--color-text-lo)', glow: 'transparent', dot: 'var(--color-text-lo)' }
  }
}

export interface JobCardProps {
  posting: PostingSummary
  /** Current per-user state (drives applied/saved treatment); NEW when untouched. */
  state?: JobState
  onState?: (state: JobState) => void
  /** True right after arrival — plays the one-time pulse (bible §6). */
  justArrived?: boolean
}

export function JobCard({ posting, state = 'NEW', onState, justArrived = false }: JobCardProps) {
  const temp = temperatureOf(posting.caughtMinutes)
  const c = rampColor(temp)
  const salary = formatSalary(posting.salary)
  const sponsor = sponsorshipBadge(posting.sponsorshipSignal)
  const src = sourceTag(posting.source)
  const applied = state === 'APPLIED'

  return (
    <article
      className="rounded-r-xl bg-card px-4 py-3 transition-colors hover:bg-card-hover"
      style={{
        borderLeft: `3px solid ${c.edge}`,
        boxShadow: temp === 'hot' || temp === 'steady' ? `0 0 12px 0 ${c.glow}` : 'none',
        animation: justArrived && temp === 'hot' ? 'fresh-pulse 1.2s ease-out' : undefined,
        opacity: applied ? 0.82 : 1,
      }}
    >
      {/* row 1: title + catch stamp */}
      <div className="flex items-baseline justify-between gap-3">
        <h3 className="text-[20px] font-semibold leading-7 text-text-hi">
          <span
            aria-hidden="true"
            className="mr-2 inline-block h-2 w-2 rounded-full align-middle"
            style={{ background: c.dot }}
          />
          {posting.url ? (
            <a href={posting.url} target="_blank" rel="noreferrer" className="hover:underline">
              {posting.title}
            </a>
          ) : (
            posting.title
          )}
        </h3>
        <span
          className="shrink-0 font-mono text-[13px] font-medium"
          style={{ color: c.stamp }}
        >
          {catchLabel(posting.caughtMinutes)}
          {temp === 'hot' ? ' ↑' : ''}
        </span>
      </div>

      {/* row 2: company · location · type */}
      <p className="mt-1 text-[13px] text-text-mid">{metaLine(posting)}</p>

      {/* row 3: badges */}
      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px]">
        {salary ? <span className="font-mono text-text-hi">{salary}</span> : null}
        <span style={{ color: sponsorTone(sponsor.tone) }}>{sponsor.label}</span>
        {posting.seniority && posting.seniority !== 'UNKNOWN' ? (
          <span className="text-text-mid">{humanize(posting.seniority)}</span>
        ) : null}
        {src ? (
          <span className="rounded border border-line px-1.5 py-0.5 font-mono text-[11px] text-text-lo">
            {src}
          </span>
        ) : null}
      </div>

      {/* row 4: actions */}
      <div className="mt-3 flex items-center justify-end gap-2">
        <CardButton label="save" active={state === 'SAVED'} onClick={() => onState?.('SAVED')} />
        <CardButton label="applied" active={applied} onClick={() => onState?.('APPLIED')} />
        <button
          type="button"
          aria-label="Hide this posting"
          onClick={() => onState?.('HIDDEN')}
          className="rounded px-2 py-1 text-[13px] text-text-lo transition-colors hover:bg-card-hover hover:text-[var(--color-danger)]"
        >
          ✕
        </button>
      </div>
    </article>
  )
}

function CardButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded border px-3 py-1 text-[13px] font-medium transition-colors"
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

function sponsorTone(tone: 'ok' | 'danger' | 'mute'): string {
  if (tone === 'ok') return 'var(--color-ok)'
  if (tone === 'danger') return 'var(--color-danger)'
  return 'var(--color-text-lo)'
}
