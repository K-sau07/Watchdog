import type { AgentStatus } from '../../lib/api'
import { agoLabel, countdownLabel } from './statusFormat'

export interface AgentPanelProps {
  status: AgentStatus | undefined
  isError: boolean
  nowMs: number
  onRefresh: () => void
  isRefreshing: boolean
  /** Non-null when the last refresh hit the cooldown — seconds to wait. */
  cooldownSeconds: number | null
}

/**
 * The agent's vital signs, as the side panel.
 *
 * This was a full-width bar pinned above the feed. In the rebuilt shell the top strip
 * belongs to navigation, and the reference product puts account state in a right-hand
 * panel — so the agent moved there. Same numbers, all DB-derived (D-WD12), and it still
 * degrades honestly when the backend is unreachable rather than showing a hopeful zero.
 */
export function AgentPanel({
  status,
  isError,
  nowMs,
  onRefresh,
  isRefreshing,
  cooldownSeconds,
}: AgentPanelProps) {
  const live = !isError && status !== undefined

  return (
    <section
      aria-label="Agent status"
      className="rounded-card border border-line bg-surface p-4 shadow-[var(--shadow-card)]"
    >
      <div className="flex items-center gap-2">
        <span
          aria-hidden="true"
          className="inline-block h-2 w-2 shrink-0 rounded-full"
          style={{
            background: live ? 'var(--color-mint)' : 'var(--color-danger)',
            animation: live ? 'heartbeat 2s ease-in-out infinite' : undefined,
          }}
        />
        <span className="text-[14px] font-semibold">
          {isError ? 'agent unreachable' : live ? `watching · ${status.boardsWatched} boards` : 'connecting…'}
        </span>
      </div>

      {live ? (
        <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-[13px]">
          <dt className="text-text-lo">last poll</dt>
          <dd className="text-right font-mono text-text-mid">{agoLabel(status.lastPoll, nowMs)}</dd>

          <dt className="text-text-lo">next poll</dt>
          <dd className="text-right font-mono text-text-mid">{countdownLabel(status.nextPoll, nowMs)}</dd>

          <dt className="text-text-lo">new today</dt>
          <dd className="text-right font-mono text-text-mid">{status.newToday}</dd>
        </dl>
      ) : null}

      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing || cooldownSeconds !== null}
        title="Poll all boards now"
        className="mt-4 w-full rounded-pill bg-panel px-3 py-2 text-[13px] font-semibold text-on-panel transition-opacity hover:opacity-90 disabled:opacity-40"
      >
        {isRefreshing
          ? 'refreshing…'
          : cooldownSeconds !== null
            ? `wait ${cooldownSeconds}s`
            : '↻ refresh'}
      </button>
    </section>
  )
}
