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
  const [filter, setFilter] = useState<FilterState>(EMPTY_FILTER)
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

  return (
    <div className="min-h-screen bg-void text-text-hi">
      <AgentStatusBar status={status.data} isError={status.isError} nowMs={nowMs} />

      <div className="flex flex-col md:flex-row">
        <FilterRail value={filter} onChange={setFilter} onReset={resetFilters} />

        <main className="flex-1 px-4 py-6 md:px-8">
          <HeroStat medianMinutes={status.data?.medianCatchMinutesToday ?? null} isLive={!status.isError} />

          <FeedList
            postings={postings}
            isLoading={feed.isLoading}
            isError={feed.isError}
            hasActiveFilters={activeCount(filter) > 0}
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
