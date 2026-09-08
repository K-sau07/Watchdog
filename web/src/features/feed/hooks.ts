import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchPostings,
  setJobState,
  type JobState,
  type PageResponse,
  type PostingSummary,
} from '../../lib/api'

/** Feed refetch cadence — a live radar feel without hammering (bible §6 heartbeat). */
const FEED_POLL_MS = 30_000

/** The postings feed for a given filter query string. Auto-refetches to stay live. */
export function usePostings(queryString: string) {
  return useQuery({
    queryKey: ['postings', queryString],
    queryFn: () => fetchPostings(queryString),
    refetchInterval: FEED_POLL_MS,
    placeholderData: (prev) => prev, // keep the old page visible while refetching
  })
}

/**
 * Job-state mutation (save/applied/hide) with optimistic UI: HIDDEN removes the card
 * from every cached feed page immediately; other states update in place. Rolls back on
 * error. Verbs stay consistent (bible §7): the action that says "applied" produces an
 * applied state.
 */
export function useSetJobState(queryString: string) {
  const qc = useQueryClient()
  const key = ['postings', queryString]

  return useMutation({
    mutationFn: ({ id, state, note }: { id: string; state: JobState; note?: string }) =>
      setJobState(id, state, note),

    onMutate: async ({ id, state }) => {
      await qc.cancelQueries({ queryKey: key })
      const prev = qc.getQueryData<PageResponse<PostingSummary>>(key)
      if (prev) {
        const next: PageResponse<PostingSummary> =
          state === 'HIDDEN'
            ? { ...prev, items: prev.items.filter((p) => p.id !== id), totalMatched: Math.max(0, prev.totalMatched - 1) }
            : prev // non-hide states don't change feed membership here
        qc.setQueryData(key, next)
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
