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
      className="sticky top-0 z-30 flex flex-wrap items-center gap-x-6 gap-y-1 border-b border-line bg-surface px-5 py-3"
      style={{ backgroundImage: 'linear-gradient(var(--color-line) 1px, transparent 1px)', backgroundSize: '100% 28px' }}
    >
      <span className="font-display text-[18px] font-semibold tracking-tight text-text-hi">
        watchdog
      </span>

      <span className="flex items-center gap-2 text-[13px] text-text-mid">
        <span
          aria-hidden="true"
          className="inline-block h-2 w-2 rounded-full"
          style={{
            background: live ? 'var(--color-amber)' : 'var(--color-danger)',
            animation: live ? 'heartbeat 2s ease-in-out infinite' : undefined,
          }}
        />
        {isError ? 'agent unreachable' : live ? `watching · ${status.boardsWatched} boards` : 'connecting…'}
      </span>

      {live ? (
        <>
          <span className="font-mono text-[13px] text-text-lo">
            last poll {agoLabel(status.lastPoll, nowMs)}
          </span>
          <span className="font-mono text-[13px] text-text-lo">
            next {countdownLabel(status.nextPoll, nowMs)}
          </span>
          <span className="ml-auto font-mono text-[13px] text-text-mid">
            {status.newToday} new today
          </span>
        </>
      ) : null}

      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing || cooldownSeconds !== null}
        title="Poll all boards now"
        className={`rounded border border-line px-3 py-1 text-[13px] transition-colors hover:bg-card-hover disabled:opacity-50 ${live ? '' : 'ml-auto'}`}
        style={{ color: 'var(--color-amber)' }}
      >
        {isRefreshing
          ? 'refreshing…'
          : cooldownSeconds !== null
            ? `wait ${cooldownSeconds}s`
            : '↻ refresh'}
      </button>
    </header>
  )
}
