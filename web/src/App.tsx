import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from './shell/AppShell'
import { AgentPanel } from './features/status/AgentPanel'
import { PacePanel } from './features/outreach/PacePanel'
import { useAgentStatus, useNowMs, useRefreshPoll } from './features/status/hooks'
import { FeedPage } from './pages/FeedPage'
import { StartedPage } from './pages/StartedPage'
import { AppliedPage } from './pages/AppliedPage'

/**
 * Routing and the one piece of state that outlives any single tab: the agent.
 *
 * The agent keeps watching whichever view is open, so its panel and its refresh
 * cooldown live here rather than inside a page that unmounts on navigation.
 */
function App() {
  const status = useAgentStatus()
  const nowMs = useNowMs()

  // On-demand refresh (D-WD19) with a cooldown surfaced from the 429 response.
  const refresh = useRefreshPoll()
  const [cooldownSeconds, setCooldownSeconds] = useState<number | null>(null)
  const doRefresh = () => {
    refresh.mutate(undefined, {
      onSuccess: (r) => {
        if (r.status === 'cooldown' && r.retryAfterSeconds) setCooldownSeconds(r.retryAfterSeconds)
      },
    })
  }
  useEffect(() => {
    if (cooldownSeconds === null || cooldownSeconds <= 0) return
    const id = setTimeout(() => setCooldownSeconds((s) => (s === null ? null : s - 1)), 1000)
    return () => clearTimeout(id)
  }, [cooldownSeconds])

  const side = (
    <>
      <AgentPanel
        status={status.data}
        isError={status.isError}
        nowMs={nowMs}
        onRefresh={doRefresh}
        isRefreshing={refresh.isPending}
        cooldownSeconds={cooldownSeconds}
      />
      <PacePanel />
    </>
  )

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShell side={side} />}>
          <Route index element={<FeedPage />} />
          <Route path="started" element={<StartedPage />} />
          <Route path="applied" element={<AppliedPage />} />
          {/* Rail destinations without pages of their own yet land on the feed rather
              than a dead route. */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
