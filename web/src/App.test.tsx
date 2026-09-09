import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'

function renderApp() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  it('renders the command bar and filter rail', () => {
    renderApp()
    expect(screen.getByText('watchdog')).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { name: /filters/i }).length).toBeGreaterThan(0)
  })

  it('shows a loading feed state before data arrives', () => {
    renderApp()
    // No backend in the test env → the feed query is pending → scanning state.
    expect(screen.getByText(/scanning boards|no catches yet|couldn't reach/i)).toBeInTheDocument()
  })

  it('opens the mobile filter drawer and closes it', () => {
    renderApp()
    // Mobile toggle shows the active-filter count (default view = "posted this week").
    fireEvent.click(screen.getByRole('button', { name: /^filters \(\d+\)$/i }))
    const close = screen.getByRole('button', { name: /close filters/i })
    expect(close).toBeInTheDocument()
    fireEvent.click(close)
    expect(screen.queryByRole('button', { name: /close filters/i })).not.toBeInTheDocument()
  })
})
