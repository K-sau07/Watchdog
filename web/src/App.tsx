import { useEffect, useMemo, useRef, useState } from 'react'
import { EMPTY_FILTER, toQueryString, activeCount, type FilterState } from './lib/filters'
import { usePostings, useSetJobState } from './features/feed/hooks'
import { FeedList } from './features/feed/FeedList'
import { FilterRail } from './features/filters/FilterRail'
import { AgentStatusBar } from './features/status/AgentStatusBar'
import { useAgentStatus, useNowMs } from './features/status/hooks'
import { medianLabel } from './features/status/statusFormat'
import type { JobState } from './lib/api'

function App() {
  // Default view: roles posted this week, so stale backfill never clutters the first screen.
  // Widen via the "posted within" filter anytime.
  const [filter, setFilter] = useState<FilterState>({ ...EMPTY_FILTER, postedWithin: 'week' })
  const queryString = useMemo(() => toQueryString(filter), [filter])

  const feed = usePostings(queryString)
  const setState = useSetJobState(queryString)
  const status = useAgentStatus()
  const nowMs = useNowMs()

  const postings = feed.data?.items ?? []

  // Track which ids are newly present since last render → play the arrival pulse once.
  const seenIds = useRef<Set<string>>(new Set())
  const [freshIds, setFreshIds] = useState<ReadonlySet<string>>(new Set())
  useEffect(() => {
    const incoming = new Set((feed.data?.items ?? []).map((p) => p.id))
    const fresh = new Set<string>()
    for (const id of incoming) if (!seenIds.current.has(id)) fresh.add(id)
    // Only flag as fresh after the very first load (avoid pulsing the whole initial page).
    if (seenIds.current.size > 0 && fresh.size > 0) setFreshIds(fresh)
    seenIds.current = incoming
  }, [feed.data])

  const onState = (id: string, state: JobState) => setState.mutate({ id, state })
  const resetFilters = () => setFilter(EMPTY_FILTER)

  const [railOpen, setRailOpen] = useState(false)
  const filterCount = activeCount(filter)

  return (
    <div className="min-h-screen bg-void text-text-hi">
      <AgentStatusBar status={status.data} isError={status.isError} nowMs={nowMs} />

      {/* Mobile-only: a bar to open the filter drawer (rail is pinned on desktop). */}
      <div className="sticky top-[49px] z-20 flex items-center gap-3 border-b border-line bg-void px-4 py-2 md:hidden">
        <button
          type="button"
          onClick={() => setRailOpen(true)}
          className="rounded border border-line px-3 py-1 text-[13px] text-text-mid transition-colors hover:bg-card-hover"
        >
          filters{filterCount > 0 ? ` (${filterCount})` : ''}
        </button>
      </div>

      <div className="flex flex-col md:flex-row">
        {/* Desktop: rail sticks under the agent bar with its own scroll. */}
        <div className="hidden md:block md:sticky md:top-[49px] md:h-[calc(100vh-49px)] md:shrink-0 md:overflow-y-auto">
          <FilterRail value={filter} onChange={setFilter} onReset={resetFilters} />
        </div>

        {/* Mobile: slide-in drawer over a scrim. */}
        {railOpen ? (
          <div className="fixed inset-0 z-20 md:hidden">
            <div
              className="absolute inset-0 bg-black/60"
              onClick={() => setRailOpen(false)}
              aria-hidden="true"
            />
            <div className="absolute inset-y-0 left-0 w-[85%] max-w-[320px] overflow-y-auto">
              <div className="flex items-center justify-between border-b border-line bg-surface px-4 py-3">
                <span className="font-display text-[15px] text-text-hi">filters</span>
                <button
                  type="button"
                  aria-label="Close filters"
                  onClick={() => setRailOpen(false)}
                  className="rounded px-2 py-1 text-text-mid hover:text-text-hi"
                >
                  ✕
                </button>
              </div>
              <FilterRail value={filter} onChange={setFilter} onReset={resetFilters} />
            </div>
          </div>
        ) : null}

        <main className="flex-1 px-4 py-6 md:px-8">
          <HeroStat medianMinutes={status.data?.medianCatchMinutesToday ?? null} isLive={!status.isError} />

          <FeedList
            postings={postings}
            isLoading={feed.isLoading}
            isError={feed.isError}
            hasActiveFilters={filterCount > 0}
            onState={onState}
            onResetFilters={resetFilters}
            onRetry={() => feed.refetch()}
            freshIds={freshIds}
          />
        </main>
      </div>
    </div>
  )
}

function HeroStat({ medianMinutes, isLive }: { medianMinutes: number | null; isLive: boolean }) {
  const [big, small] = splitMedian(medianLabel(medianMinutes))
  return (
    <div className="mb-6">
      <p className="font-display text-[40px] font-semibold leading-[44px] text-text-hi">
        {big}
        {small ? <span className="ml-2 align-middle text-[15px] font-normal text-text-mid">{small}</span> : null}
      </p>
      {isLive && medianMinutes !== null ? (
        <p className="mt-1 font-voice text-[15px] italic text-text-mid">
          you're seeing these before most applicants
        </p>
      ) : null}
    </div>
  )
}

/** Split "median catch time today · 6 min" → ["6 min", "median catch time today"] for the
 *  big-number-first hero treatment; falls back gracefully when there's no data. */
function splitMedian(label: string): [string, string | null] {
  const parts = label.split(' · ')
  if (parts.length === 2) return [parts[1], parts[0]]
  return [label, null]
}

export default App
