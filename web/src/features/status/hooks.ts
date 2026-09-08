import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchAgentStatus } from '../../lib/api'

/** Agent status, refetched on the poll cadence so the bar reflects real activity. */
export function useAgentStatus() {
  return useQuery({
    queryKey: ['agent-status'],
    queryFn: fetchAgentStatus,
    refetchInterval: 15_000,
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
