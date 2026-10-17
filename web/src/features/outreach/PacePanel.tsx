import { usePace } from './hooks'

/**
 * This week's outreach budget.
 *
 * 20 companies at 8 people is 160 invites against an allowance of 200. The bar is the
 * stop signal: when the invites are burned there is no more LinkedIn outreach this week
 * regardless of how many companies are left, so the number that matters is how much of
 * the allowance is gone rather than how much work was done.
 */
export function PacePanel() {
  const pace = usePace()

  // Silent until there is something to report. An empty budget bar on a fresh install
  // is noise, and the panel sits under the agent's own readout where space is earned.
  if (!pace.data || pace.data.invitesSent === 0) return null

  const { invitesSent, inviteCap, companiesTouched, companiesTarget, invitesBurned } = pace.data
  const pct = Math.min(100, Math.round((invitesSent / Math.max(1, inviteCap)) * 100))

  return (
    <section
      aria-label="This week's outreach"
      className="mt-4 rounded-card border border-line bg-surface p-4 shadow-[var(--shadow-card)]"
    >
      <h3 className="text-[14px] font-semibold text-text-hi">this week</h3>

      <div className="mt-3 flex items-baseline justify-between text-[13px]">
        <span className="text-text-lo">invites</span>
        <span className="font-mono text-text-mid">
          {invitesSent} / {inviteCap}
        </span>
      </div>
      <div className="mt-1.5 h-1.5 overflow-hidden rounded-pill bg-sunken">
        <div
          className="h-full rounded-pill transition-[width]"
          style={{
            width: `${pct}%`,
            background: invitesBurned ? 'var(--color-danger)' : 'var(--color-mint)',
          }}
        />
      </div>

      <div className="mt-3 flex items-baseline justify-between text-[13px]">
        <span className="text-text-lo">companies</span>
        <span className="font-mono text-text-mid">
          {companiesTouched} / {companiesTarget}
        </span>
      </div>

      {invitesBurned ? (
        <p className="mt-3 text-[12px] text-danger">
          invites spent for the week — email only from here
        </p>
      ) : null}
    </section>
  )
}
