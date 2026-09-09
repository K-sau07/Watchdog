import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAgentStatus, triggerPoll, type PollResult } from '../../lib/api'

/** Agent status, refetched on the poll cadence so the bar reflects real activity. */
export function useAgentStatus() {
  return useQuery({
    queryKey: ['agent-status'],
    queryFn: fetchAgentStatus,
    refetchInterval: 15_000,
  })
}

/**
 * On-demand "refresh now" (D-WD19). Triggers a poll; on a real poll it refetches the feed
 * + status so new roles appear immediately. A cooldown response is returned (not thrown)
 * so the UI can show "wait Ns".
 */
export function useRefreshPoll() {
  const qc = useQueryClient()
  return useMutation<PollResult>({
    mutationFn: triggerPoll,
    onSuccess: (result) => {
      if (result.status === 'polled') {
        qc.invalidateQueries({ queryKey: ['postings'] })
        qc.invalidateQueries({ queryKey: ['agent-status'] })
      }
    },
  })
}

/** A 1s ticking clock (ms) so relative labels ("38s ago", "in 1:22") stay live. */
export function useNowMs(): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])
  return now
}
