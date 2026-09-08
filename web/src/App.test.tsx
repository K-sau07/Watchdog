import { render, screen } from '@testing-library/react'
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
    expect(screen.getByRole('heading', { name: /filters/i })).toBeInTheDocument()
  })

  it('shows a loading feed state before data arrives', () => {
    renderApp()
    // No backend in the test env → the feed query is pending → scanning state.
    expect(screen.getByText(/scanning boards|no catches yet|couldn't reach/i)).toBeInTheDocument()
  })
})
