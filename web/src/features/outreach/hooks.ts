import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchOutreach, fetchFollowUps, fetchPace, targetPerson, recordEvent, patchContact } from '../../lib/api'

const BOARD = ['outreach']
const FOLLOW_UPS = ['outreach', 'follow-ups']
const PACE = ['outreach', 'pace']

export function useOutreach() {
  return useQuery({ queryKey: BOARD, queryFn: fetchOutreach })
}

export function useFollowUps() {
  return useQuery({ queryKey: FOLLOW_UPS, queryFn: fetchFollowUps })
}

export function usePace() {
  return useQuery({ queryKey: PACE, queryFn: fetchPace })
}

/**
 * Any write invalidates all three views. They are projections of one event log, so a
 * touch recorded on the board changes the follow-up queue and the week's pace too —
 * refreshing only the view you were looking at would leave the other two stale.
 */
function invalidateAll(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: BOARD })
  qc.invalidateQueries({ queryKey: FOLLOW_UPS })
  qc.invalidateQueries({ queryKey: PACE })
}

export function useTargetPerson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: targetPerson,
    onSettled: () => invalidateAll(qc),
  })
}

export function useRecordEvent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...input }: Parameters<typeof recordEvent>[1] & { id: string }) =>
      recordEvent(id, input),
    onSettled: () => invalidateAll(qc),
  })
}

export function usePatchContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...patch }: Parameters<typeof patchContact>[1] & { id: string }) =>
      patchContact(id, patch),
    onSettled: () => invalidateAll(qc),
  })
}
