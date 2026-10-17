import { render, screen, fireEvent, within } from '@testing-library/react'
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
  it('renders the rail and the view tabs', () => {
    renderApp()
    // The rail renders twice (desktop column + mobile bottom bar), so these are
    // getAllBy — both are in the DOM at once and CSS decides which is visible.
    expect(screen.getAllByRole('navigation', { name: /sections/i }).length).toBe(2)

    // Scoped to the tab strip: "Started" and "Applied" also appear in both rails, so an
    // unscoped query matches three nodes and tells us nothing about the tabs.
    const views = within(screen.getByRole('navigation', { name: /views/i }))
    expect(views.getByRole('link', { name: /^feed$/i })).toBeInTheDocument()
    expect(views.getByRole('link', { name: /^started$/i })).toBeInTheDocument()
    expect(views.getByRole('link', { name: /^applied$/i })).toBeInTheDocument()
  })

  it('shows a loading feed state before data arrives', () => {
    renderApp()
    // No backend in the test env → the feed query is pending → scanning state.
    expect(screen.getByText(/scanning boards|no catches yet|couldn't reach/i)).toBeInTheDocument()
  })

  it('opens a filter pill and closes it on Escape', () => {
    renderApp()
    const pill = screen.getByRole('button', { name: /funding stage/i })
    fireEvent.click(pill)
    expect(pill).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('checkbox', { name: /series A$/i })).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(pill).toHaveAttribute('aria-expanded', 'false')
  })

  it('names the section with the page\'s only h1', () => {
    renderApp()
    const h1s = screen.getAllByRole('heading', { level: 1 })
    expect(h1s).toHaveLength(1)
    expect(h1s[0]).toHaveTextContent('JOBS')
  })

  it('gives the hero stat a readable accessible name', () => {
    renderApp()
    // The number and its label are separated visually by a margin, not whitespace, so
    // without an explicit label a screen reader runs them together.
    const hero = screen
      .getAllByRole('heading', { level: 2 })
      .find((h) => /median catch time|no catches|—/.test(h.getAttribute('aria-label') ?? ''))
    expect(hero).toBeDefined()
    expect(hero!.getAttribute('aria-label')).not.toMatch(/\w—\w|daysmedian/)
  })
})
