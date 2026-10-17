import { useEffect, useMemo, useRef, useState } from 'react'
import { EMPTY_FILTER, toQueryString, activeCount, type FilterState } from '../lib/filters'
import { usePostings, useSetJobState, flattenPages } from '../features/feed/hooks'
import { FeedList } from '../features/feed/FeedList'
import { FilterBar } from '../features/filters/FilterBar'
import { useAgentStatus } from '../features/status/hooks'
import { heroStat, hasEarlyAdvantage } from '../features/status/statusFormat'
import type { JobState } from '../lib/api'

/**
 * The feed: fresh postings, newest first, with the pill bar driving the query.
 *
 * This is the top of the funnel — a company is picked here and then carried into
 * Started. Everything the feed filters on is server-side (spec §5), so the pill bar
 * changes the request rather than hiding rows that were already fetched.
 */
export function FeedPage() {
  // Default view: US roles posted this week, so stale/foreign backfill never clutters the
  // first screen. Widen via the pill bar.
  const [filter, setFilter] = useState<FilterState>({ ...EMPTY_FILTER, postedWithin: 'week', usOnly: true })
  const queryString = useMemo(() => toQueryString(filter), [filter])

  const feed = usePostings(queryString)
  const setState = useSetJobState(queryString)
  const status = useAgentStatus()

  const postings = useMemo(() => flattenPages(feed.data?.pages), [feed.data])

  // Track which ids are newly present since last render → play the arrival pulse once.
  const seenIds = useRef<Set<string>>(new Set())
  const [freshIds, setFreshIds] = useState<ReadonlySet<string>>(new Set())
  useEffect(() => {
    const incoming = new Set(postings.map((p) => p.id))
    const fresh = new Set<string>()
    for (const id of incoming) if (!seenIds.current.has(id)) fresh.add(id)
    // Only flag as fresh after the very first load (avoid pulsing the whole initial page).
    if (seenIds.current.size > 0 && fresh.size > 0) setFreshIds(fresh)
    seenIds.current = incoming
  }, [postings])

  const onState = (id: string, state: JobState) => setState.mutate({ id, state })
  const resetFilters = () => setFilter(EMPTY_FILTER)
  const filterCount = activeCount(filter)

  return (
    <>
      <HeroStat
        medianMinutes={status.data?.medianCatchMinutesToday ?? null}
        caughtToday={status.data?.caughtToday ?? 0}
        newToday={status.data?.newToday ?? 0}
        isLive={!status.isError}
      />

      <FilterBar value={filter} onChange={setFilter} onReset={resetFilters} />

      <FeedList
        postings={postings}
        isLoading={feed.isLoading}
        isError={feed.isError}
        hasActiveFilters={filterCount > 0}
        onState={onState}
        onResetFilters={resetFilters}
        onRetry={() => feed.refetch()}
        freshIds={freshIds}
        hasMore={feed.hasNextPage}
        isFetchingMore={feed.isFetchingNextPage}
        onLoadMore={() => feed.fetchNextPage()}
      />
    </>
  )
}

function HeroStat({
  medianMinutes,
  caughtToday,
  newToday,
  isLive,
}: {
  medianMinutes: number | null
  caughtToday: number
  newToday: number
  isLive: boolean
}) {
  const hero = heroStat(medianMinutes, caughtToday, newToday)
  return (
    <div className="mb-5">
      {/* The label is positioned with a margin, not a space, so the two spans ran together
          for a screen reader ("4 daysmedian catch time today"). aria-label carries the
          readable sentence and leaves the visual treatment untouched. */}
      <h2
        aria-label={hero.label ? `${hero.big} — ${hero.label}` : hero.big}
        className="text-[34px] font-bold leading-[38px] tracking-tight text-text-hi"
      >
        <span aria-hidden="true">{hero.big}</span>
        {hero.label ? (
          <span aria-hidden="true" className="ml-2 align-middle text-[14px] font-normal text-text-lo">
            {hero.label}
          </span>
        ) : null}
      </h2>
      {isLive && hasEarlyAdvantage(medianMinutes) && caughtToday > 0 ? (
        <p className="mt-1 text-[14px] text-text-mid">you're seeing these before most applicants</p>
      ) : isLive && hero.note ? (
        <p className="mt-1 max-w-[60ch] text-[13px] text-text-lo">{hero.note}</p>
      ) : null}
    </div>
  )
}
