import type { AgentStatus } from '../../lib/api'
import { agoLabel, countdownLabel } from './statusFormat'

export interface AgentStatusBarProps {
  status: AgentStatus | undefined
  isError: boolean
  nowMs: number
}

/**
 * The top command bar (bible §4/§7): a live "watching" heartbeat, boards watched, and the
 * poll cadence. All numbers are real (DB-derived backend, D-WD12). Degrades honestly when
 * the agent is unreachable.
 */
export function AgentStatusBar({ status, isError, nowMs }: AgentStatusBarProps) {
  const live = !isError && status !== undefined
  return (
    <header
      className="flex flex-wrap items-center gap-x-6 gap-y-1 border-b border-line bg-surface px-5 py-3"
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
    </header>
  )
}
