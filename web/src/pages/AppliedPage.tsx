import { useMemo } from 'react'
import { EMPTY_FILTER, toQueryString, type FilterState } from '../lib/filters'
import { usePostings, useSetJobState, flattenPages } from '../features/feed/hooks'
import { FeedList } from '../features/feed/FeedList'
import type { JobState } from '../lib/api'

/**
 * Roles applied to directly on the company's own site.
 *
 * No new storage behind this: APPLIED is already a job state the backend persists
 * (S7, PUT /api/postings/{id}/state), so this page is the same feed read back with
 * state=APPLIED. Applying happens on their site — Watchdog records that it happened,
 * it doesn't pretend to submit anything.
 */
export function AppliedPage() {
  const filter: FilterState = useMemo(() => ({ ...EMPTY_FILTER, state: ['APPLIED'] }), [])
  const queryString = useMemo(() => toQueryString(filter), [filter])

  const feed = usePostings(queryString)
  const setState = useSetJobState(queryString)
  const postings = useMemo(() => flattenPages(feed.data?.pages), [feed.data])

  const onState = (id: string, state: JobState) => setState.mutate({ id, state })

  return (
    <>
      <PageHead
        title="Applied"
        count={postings.length}
        blurb="roles you applied to on the company's own site"
      />
      <FeedList
        postings={postings}
        isLoading={feed.isLoading}
        isError={feed.isError}
        hasActiveFilters={false}
        onState={onState}
        onResetFilters={() => {}}
        onRetry={() => feed.refetch()}
        hasMore={feed.hasNextPage}
        isFetchingMore={feed.isFetchingNextPage}
        onLoadMore={() => feed.fetchNextPage()}
      />
    </>
  )
}

export function PageHead({
  title,
  count,
  blurb,
}: {
  title: string
  count?: number
  blurb: string
}) {
  return (
    <div className="mb-5">
      <h2 className="text-[26px] font-bold tracking-tight text-text-hi">
        {title}
        {count !== undefined ? (
          <span className="ml-2 align-middle text-[15px] font-normal text-text-lo">{count}</span>
        ) : null}
      </h2>
      <p className="mt-1 text-[14px] text-text-mid">{blurb}</p>
    </div>
  )
}
