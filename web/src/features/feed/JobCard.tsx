import type { PostingSummary } from '../../lib/api'
import type { JobState } from '../../lib/api'
import { catchLabel, temperatureOf, isFresh } from '../../lib/freshness'
import { formatSalary, humanize, sourceTag } from './cardFormat'
import { BanIcon, HeartIcon, LevelIcon, MoneyIcon, PinIcon, RemoteIcon, TypeIcon } from './icons'
import { ReachOutButton } from '../outreach/ReachOutButton'

/**
 * Funding stage as it appears on a card. Returns null for UNKNOWN and for a missing
 * company: a blank badge would read as a claim, and no stage on record is not a stage.
 */
function stageLabel(stage: string | null): string | null {
  switch (stage) {
    case 'SEED':
      return 'Seed'
    case 'SERIES_A':
      return 'Series A'
    case 'SERIES_B_PLUS':
      return 'Series B+'
    case 'GROWTH':
      return 'Late Stage'
    default:
      return null
  }
}

/** Initial-block stand-in for a company logo, which no ATS feed gives us. */
function logoInitials(name: string | null): string {
  if (!name) return '?'
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[1][0]).toUpperCase()
}

export interface JobCardProps {
  posting: PostingSummary
  /** Current per-user state (drives applied/saved treatment); NEW when untouched. */
  state?: JobState
  onState?: (state: JobState) => void
  /** True right after arrival — plays the one-time pulse. */
  justArrived?: boolean
}

export function JobCard({ posting, state = 'NEW', onState, justArrived = false }: JobCardProps) {
  const temp = temperatureOf(posting.caughtMinutes)
  const salary = formatSalary(posting.salary)
  const stage = stageLabel(posting.fundingStage)
  const src = sourceTag(posting.source)
  const applied = state === 'APPLIED'
  const saved = state === 'SAVED'
  const early = isFresh(posting.caughtMinutes)

  return (
    <article
      className="flex flex-col overflow-hidden rounded-[16px] bg-surface shadow-[var(--shadow-card)] ring-1 ring-line transition-shadow hover:shadow-[var(--shadow-raised)] md:flex-row"
      style={{
        animation: justArrived ? 'fresh-pulse 1.4s ease-out' : undefined,
        opacity: applied ? 0.75 : 1,
      }}
    >
      {/* --- body --- */}
      <div className="flex min-w-0 flex-1 flex-col p-4">
        <div className="flex min-w-0 gap-3.5">
          <span
            aria-hidden="true"
            className="mt-0.5 grid h-14 w-14 shrink-0 place-items-center rounded-[12px] bg-sunken font-display text-[17px] font-semibold text-text-lo"
          >
            {logoInitials(posting.companyName)}
          </span>

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5">
              <Chip tone={early ? 'mint' : 'plain'}>{catchLabel(posting.caughtMinutes)}</Chip>
              {early ? <Chip tone="mint">Early applicant</Chip> : null}
              {stage ? <Chip tone="plain">{stage}</Chip> : null}
            </div>

            <h3 className="mt-1.5 truncate font-display text-[19px] font-semibold leading-7 text-text-hi">
              {posting.url ? (
                <a href={posting.url} target="_blank" rel="noreferrer" className="hover:underline">
                  {posting.title}
                </a>
              ) : (
                posting.title
              )}
            </h3>

            <p className="mt-0.5 truncate text-[13px] text-text-lo">
              <span className="font-medium text-text-mid">{posting.companyName ?? 'unknown company'}</span>
              {src ? <> {' / '}{src}</> : null}
            </p>
          </div>
        </div>

        <hr className="my-3 border-0 border-t border-line" />

        {/* Meta grid — the reference packs six facts into two rows of three. */}
        <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-[13px] text-text-mid sm:grid-cols-2 lg:grid-cols-3">
          <Fact icon={<PinIcon />} label="location">{posting.location ?? '—'}</Fact>
          <Fact icon={<TypeIcon />} label="employment type">
            {posting.employmentType && posting.employmentType !== 'UNKNOWN'
              ? humanize(posting.employmentType)
              : '—'}
          </Fact>
          <Fact icon={<MoneyIcon />} label="salary">{salary ?? 'not stated'}</Fact>
          <Fact icon={<RemoteIcon />} label="work model">
            {posting.remoteType && posting.remoteType !== 'UNKNOWN' ? humanize(posting.remoteType) : '—'}
          </Fact>
          <Fact icon={<LevelIcon />} label="seniority">
            {posting.seniority && posting.seniority !== 'UNKNOWN' ? humanize(posting.seniority) : '—'}
          </Fact>
          <Fact icon={<TypeIcon />} label="sponsorship">
            {posting.sponsorshipSignal === 'OFFERED'
              ? 'Sponsorship offered'
              : posting.sponsorshipSignal === 'NOT_OFFERED'
                ? 'No sponsorship'
                : 'Not stated'}
          </Fact>
        </dl>

        <hr className="my-3 border-0 border-t border-line" />

        <div className="flex items-center justify-between gap-3">
          <span className="truncate text-[12px] text-text-faint">
            {applied ? 'applied' : saved ? 'saved' : 'not applied yet'}
          </span>

          <div className="flex shrink-0 items-center gap-2">
            <IconButton label="Hide this posting" onClick={() => onState?.('HIDDEN')}>
              <BanIcon />
            </IconButton>
            <IconButton
              label={saved ? 'Unsave this posting' : 'Save this posting'}
              active={saved}
              onClick={() => onState?.('SAVED')}
            >
              <HeartIcon filled={saved} />
            </IconButton>
            <ReachOutButton companyName={posting.companyName} postingId={posting.id} />
            <button
              type="button"
              onClick={() => onState?.('APPLIED')}
              className="rounded-[var(--radius-pill)] px-3.5 py-2 text-[13px] font-semibold transition-colors"
              style={{
                background: applied ? 'var(--color-sunken)' : 'transparent',
                color: applied ? 'var(--color-text-lo)' : 'var(--color-text-mid)',
                boxShadow: applied ? 'none' : 'inset 0 0 0 1px var(--color-line-strong)',
              }}
            >
              {applied ? 'applied' : 'mark applied'}
            </button>
            {posting.url ? (
              <a
                href={posting.url}
                target="_blank"
                rel="noreferrer"
                className="rounded-[var(--radius-pill)] bg-mint px-4 py-2 text-[13px] font-semibold text-mint-ink transition-opacity hover:opacity-85"
              >
                APPLY NOW
              </a>
            ) : null}
          </div>
        </div>
      </div>

      {/* --- the one number worth staring at ---
          The reference puts a fabricated "match %" here. We have no matching model, so
          this panel carries the thing we can actually prove: how early we caught it. */}
      <aside className="flex shrink-0 flex-col justify-center gap-1 bg-panel px-5 py-4 text-on-panel md:w-[188px] md:rounded-l-none">
        <p className="font-display text-[26px] font-semibold leading-8">
          {catchLabel(posting.caughtMinutes).replace(/^caught /, '')}
        </p>
        <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-white/55">
          {temp === 'hot' || temp === 'steady'
            ? 'ahead of the crowd'
            : temp === 'unknown'
              ? 'post time unknown'
              : 'after posting'}
        </p>
        <ul className="mt-2 space-y-1 text-[11.5px] text-white/70">
          {stage ? <li>✓ {stage}</li> : null}
          {posting.sponsorshipSignal === 'OFFERED' ? <li>✓ Sponsorship offered</li> : null}
          {early ? <li>✓ Early applicant</li> : null}
        </ul>
      </aside>
    </article>
  )
}

function Fact({
  icon,
  label,
  children,
}: {
  icon: React.ReactNode
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="flex min-w-0 items-center gap-1.5">
      <span aria-hidden="true" className="shrink-0 text-text-faint">
        {icon}
      </span>
      <dt className="sr-only">{label}</dt>
      <dd className="m-0 truncate">{children}</dd>
    </div>
  )
}

function Chip({ children, tone }: { children: React.ReactNode; tone: 'mint' | 'plain' }) {
  return (
    <span
      className="rounded-[6px] px-1.5 py-0.5 text-[11px] font-medium"
      style={
        tone === 'mint'
          ? { background: 'var(--color-mint-soft)', color: 'var(--color-mint-ink)' }
          : { background: 'var(--color-badge)', color: 'var(--color-text-lo)' }
      }
    >
      {children}
    </span>
  )
}

function IconButton({
  label,
  children,
  onClick,
  active = false,
}: {
  label: string
  children: React.ReactNode
  onClick: () => void
  active?: boolean
}) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={active}
      onClick={onClick}
      className="grid h-9 w-9 place-items-center rounded-full transition-colors hover:bg-hover"
      style={{
        boxShadow: 'inset 0 0 0 1px var(--color-line-strong)',
        color: active ? 'var(--color-danger)' : 'var(--color-text-lo)',
      }}
    >
      {children}
    </button>
  )
}
