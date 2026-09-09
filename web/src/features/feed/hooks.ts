import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  fetchPostings,
  setJobState,
  type JobState,
  type PageResponse,
  type PostingSummary,
} from '../../lib/api'

/** Feed refetch cadence — a live radar feel without hammering (bible §6 heartbeat). */
const FEED_POLL_MS = 30_000
const PAGE_SIZE = 25

/**
 * The postings feed for a filter query, paginated with "load more" (D-WD13) via
 * useInfiniteQuery. Pages accumulate; fetchNextPage appends the next 25. The first page
 * auto-refetches on the poll cadence to stay live.
 */
export function usePostings(queryString: string) {
  return useInfiniteQuery({
    queryKey: ['postings', queryString],
    queryFn: ({ pageParam }) => {
      const sep = queryString ? '&' : ''
      return fetchPostings(`${queryString}${sep}page=${pageParam}&size=${PAGE_SIZE}`)
    },
    initialPageParam: 0,
    getNextPageParam: (last: PageResponse<PostingSummary>) =>
      last.page + 1 < last.totalPages ? last.page + 1 : undefined,
    refetchInterval: FEED_POLL_MS,
  })
}

/** Flatten accumulated pages into a single postings list. */
export function flattenPages(
  pages: PageResponse<PostingSummary>[] | undefined,
): PostingSummary[] {
  return pages ? pages.flatMap((p) => p.items) : []
}

/**
 * Job-state mutation (save/applied/hide). HIDDEN optimistically removes the card from
 * every accumulated page; other states are reconciled on settle. Rolls back on error.
 */
export function useSetJobState(queryString: string) {
  const qc = useQueryClient()
  const key = ['postings', queryString]

  return useMutation({
    mutationFn: ({ id, state, note }: { id: string; state: JobState; note?: string }) =>
      setJobState(id, state, note),

    onMutate: async ({ id, state }) => {
      await qc.cancelQueries({ queryKey: key })
      const prev = qc.getQueryData<{ pages: PageResponse<PostingSummary>[]; pageParams: unknown[] }>(key)
      if (prev && state === 'HIDDEN') {
        qc.setQueryData(key, {
          ...prev,
          pages: prev.pages.map((pg) => ({
            ...pg,
            items: pg.items.filter((p) => p.id !== id),
            totalMatched: Math.max(0, pg.totalMatched - 1),
          })),
        })
      }
      return { prev }
    },

    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) qc.setQueryData(key, ctx.prev)
    },

    onSettled: () => {
      qc.invalidateQueries({ queryKey: key })
    },
  })
}
