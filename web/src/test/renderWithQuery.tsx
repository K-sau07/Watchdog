import type { ReactElement, ReactNode } from 'react'
import { render, type RenderOptions, type RenderResult } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

/**
 * Render a component that reaches the API somewhere in its tree.
 *
 * A job card owns a "reach out" action, which owns a mutation, so the card can no
 * longer be rendered bare. Retries are off so a failing request surfaces immediately
 * instead of being swallowed by backoff, and each call gets a fresh client so one
 * test's cache can never answer another test's query.
 */
export function renderWithQuery(ui: ReactElement, options?: RenderOptions): RenderResult {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  const wrap = (node: ReactNode) => <QueryClientProvider client={qc}>{node}</QueryClientProvider>

  const result = render(wrap(ui), options)

  // Testing Library's rerender replaces the entire tree with exactly what it is given,
  // so handing back the raw one would drop the provider on the second render and throw.
  // Re-wrapping keeps the same client, which is also what a real rerender does.
  return { ...result, rerender: (next: ReactNode) => result.rerender(wrap(next)) }
}
