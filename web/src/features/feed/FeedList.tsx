import type { PostingSummary, JobState } from '../../lib/api'
import { groupByCompany } from '../../lib/grouping'
import { CompanySection } from './CompanySection'

export interface FeedListProps {
  postings: PostingSummary[]
  isLoading: boolean
  isError: boolean
  /** True when the active filter constrains results (drives the empty-state copy). */
  hasActiveFilters: boolean
  onState: (id: string, state: JobState) => void
  onResetFilters: () => void
  onRetry: () => void
  /** IDs that just arrived this refetch — play the one-time pulse (bible §6). */
  freshIds?: ReadonlySet<string>
  /** Pagination (D-WD13): more pages available + how to request the next. */
  hasMore: boolean
  isFetchingMore: boolean
  onLoadMore: () => void
}

export function FeedList({
  postings,
  isLoading,
  isError,
  hasActiveFilters,
  onState,
  onResetFilters,
  onRetry,
  freshIds,
  hasMore,
  isFetchingMore,
  onLoadMore,
}: FeedListProps) {
  if (isError) {
    return (
      <Panel>
        <p className="text-text-hi">couldn't reach the agent</p>
        <p className="mt-1 text-sm text-text-mid">the server didn't respond. it may be starting up.</p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 rounded border border-line px-3 py-1 text-sm text-text-mid transition-colors hover:bg-card-hover"
        >
          retry
        </button>
      </Panel>
    )
  }

  if (isLoading && postings.length === 0) {
    return (
      <Panel>
        <p className="text-text-mid">scanning boards…</p>
      </Panel>
    )
  }

  if (postings.length === 0) {
    return hasActiveFilters ? (
      <Panel>
        <p className="text-text-hi">nothing matches these filters</p>
        <p className="mt-1 text-sm text-text-mid">loosen a constraint to widen the net.</p>
        <button
          type="button"
          onClick={onResetFilters}
          className="mt-3 rounded border border-line px-3 py-1 text-sm text-text-mid transition-colors hover:bg-card-hover"
        >
          reset filters
        </button>
      </Panel>
    ) : (
      <Panel>
        <p className="text-text-hi">no catches yet</p>
        <p className="mt-1 text-sm text-text-mid">
          the agent is watching — new roles surface here the moment they post.
        </p>
      </Panel>
    )
  }

  const groups = groupByCompany(postings)

  return (
    <div className="flex flex-col gap-2.5">
      {groups.map((g) => (
        <CompanySection key={g.company} group={g} onState={onState} freshIds={freshIds} />
      ))}

      {hasMore ? (
        <button
          type="button"
          onClick={onLoadMore}
          disabled={isFetchingMore}
          className="mt-2 self-center rounded border border-line px-5 py-2 text-[13px] text-text-mid transition-colors hover:bg-card-hover disabled:opacity-50"
        >
          {isFetchingMore ? 'loading…' : 'load more'}
        </button>
      ) : (
        <p className="mt-2 text-center text-[12px] text-text-lo">that's everything matching your filters</p>
      )}
    </div>
  )
}

function Panel({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-line bg-surface px-5 py-8 text-center">{children}</div>
  )
}
