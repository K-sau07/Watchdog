import type { AgentStatus } from '../../lib/api'
import { agoLabel, countdownLabel } from './statusFormat'

export interface AgentStatusBarProps {
  status: AgentStatus | undefined
  isError: boolean
  nowMs: number
  onRefresh: () => void
  isRefreshing: boolean
  /** Non-null when the last refresh hit the cooldown — seconds to wait. */
  cooldownSeconds: number | null
}

/**
 * The top command bar (bible §4/§7): a live "watching" heartbeat, boards watched, the
 * poll cadence, and an on-demand refresh (D-WD19). All numbers are real (DB-derived
 * backend, D-WD12). Degrades honestly when the agent is unreachable.
 */
export function AgentStatusBar({
  status,
  isError,
  nowMs,
  onRefresh,
  isRefreshing,
  cooldownSeconds,
}: AgentStatusBarProps) {
  const live = !isError && status !== undefined
  return (
    <header
      /* One row, always. The ruled background is a 28px repeat sized for a single-row
         bar — letting the contents wrap put rules straight through the text, and App
         pins the filter rail at top-[49px], which a taller bar silently broke. Secondary
         readouts drop out by breakpoint instead of wrapping. */
      className="sticky top-0 z-30 flex h-[49px] items-center gap-x-4 border-b border-line bg-surface px-4 md:gap-x-6 md:px-5"
      style={{ backgroundImage: 'linear-gradient(var(--color-line) 1px, transparent 1px)', backgroundSize: '100% 28px' }}
    >
      <span className="shrink-0 font-display text-[18px] font-semibold tracking-tight text-text-hi">
        watchdog
      </span>

      <span className="flex min-w-0 items-center gap-2 text-[13px] text-text-mid">
        <span
          aria-hidden="true"
          className="inline-block h-2 w-2 shrink-0 rounded-full"
          style={{
            background: live ? 'var(--color-amber)' : 'var(--color-danger)',
            animation: live ? 'heartbeat 2s ease-in-out infinite' : undefined,
          }}
        />
        <span className="truncate">
          {isError ? 'agent unreachable' : live ? `watching · ${status.boardsWatched} boards` : 'connecting…'}
        </span>
      </span>

      {live ? (
        <>
          <span className="hidden shrink-0 font-mono text-[13px] text-text-lo lg:inline">
            last poll {agoLabel(status.lastPoll, nowMs)}
          </span>
          <span className="hidden shrink-0 font-mono text-[13px] text-text-lo lg:inline">
            next {countdownLabel(status.nextPoll, nowMs)}
          </span>
        </>
      ) : null}

      <div className="ml-auto flex shrink-0 items-center gap-3 md:gap-4">
        {live ? (
          <span className="hidden font-mono text-[13px] text-text-mid sm:inline">
            {status.newToday} new today
          </span>
        ) : null}

        <button
          type="button"
          onClick={onRefresh}
          disabled={isRefreshing || cooldownSeconds !== null}
          title="Poll all boards now"
          className="rounded border border-line px-3 py-1 text-[13px] transition-colors hover:bg-card-hover disabled:opacity-50"
          style={{ color: 'var(--color-amber)' }}
        >
          {isRefreshing
            ? 'refreshing…'
            : cooldownSeconds !== null
              ? `wait ${cooldownSeconds}s`
              : '↻ refresh'}
        </button>
      </div>
    </header>
  )
}
