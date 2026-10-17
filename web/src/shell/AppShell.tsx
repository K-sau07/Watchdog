import { NavLink, Outlet } from 'react-router-dom'
import { RadarIcon, ConversationIcon, AppliedIcon, AgentIcon, SettingsIcon } from './navIcons'

/**
 * The three-column shell: rail, work area, side panel.
 *
 * The rail is the product's table of contents and the top tabs are the three states a
 * posting moves through — caught, reached out about, applied to. Those are the same
 * three nouns the backend already stores, so the navigation isn't a layer on top of the
 * model, it *is* the model.
 *
 * Deliberately absent: a resume page, a coaching page, an interview page. The reference
 * product has all three. We have no data behind any of them, and a tab that can only
 * ever render an empty state is worse than no tab.
 */
export function AppShell({ side }: { side?: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-page text-text-hi">
      <Sidebar />
      {/* Offset by the rail's width on desktop; the rail collapses to a bottom bar below md. */}
      <div className="md:pl-[240px]">
        <TopBar />
        <div className="flex items-start gap-6 px-4 py-6 md:px-6">
          <main className="min-w-0 flex-1">
            <Outlet />
          </main>
          {side ? (
            <aside className="hidden w-[340px] shrink-0 xl:block">{side}</aside>
          ) : null}
        </div>
      </div>
    </div>
  )
}

const NAV = [
  { to: '/', label: 'Jobs', Icon: RadarIcon, end: true },
  { to: '/started', label: 'Started', Icon: ConversationIcon, end: false },
  { to: '/applied', label: 'Applied', Icon: AppliedIcon, end: false },
  { to: '/agent', label: 'Agent', Icon: AgentIcon, end: false },
]

function Sidebar() {
  return (
    <>
      {/* Desktop rail. */}
      <nav
        aria-label="Sections"
        className="fixed inset-y-0 left-0 z-30 hidden w-[240px] flex-col border-r border-line bg-surface md:flex"
      >
        <div className="flex h-16 items-center gap-2 px-5">
          <WatchdogMark />
          <span className="text-[19px] font-semibold tracking-tight">watchdog</span>
        </div>

        <div className="flex flex-col gap-0.5 px-3 pt-2">
          {NAV.map(({ to, label, Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={railItem}>
              <Icon />
              {label}
            </NavLink>
          ))}
        </div>

        <div className="mt-auto flex flex-col gap-0.5 px-3 pb-4">
          <NavLink to="/settings" className={railItem}>
            <SettingsIcon />
            Settings
          </NavLink>
        </div>
      </nav>

      {/* Below md the rail becomes a bottom bar — the same four destinations, thumb-height. */}
      <nav
        aria-label="Sections"
        className="fixed inset-x-0 bottom-0 z-30 flex border-t border-line bg-surface md:hidden"
      >
        {NAV.map(({ to, label, Icon, end }) => (
          <NavLink key={to} to={to} end={end} className={barItem}>
            <Icon />
            <span className="text-[11px]">{label}</span>
          </NavLink>
        ))}
      </nav>
    </>
  )
}

/** Active state is a tinted block, not a mint fill — mint is reserved for actions. */
const railItem = ({ isActive }: { isActive: boolean }) =>
  [
    'flex items-center gap-3 rounded-control px-3 py-2 text-[14px] transition-colors',
    isActive ? 'bg-sunken font-semibold text-text-hi' : 'text-text-mid hover:bg-hover hover:text-text-hi',
  ].join(' ')

const barItem = ({ isActive }: { isActive: boolean }) =>
  [
    'flex flex-1 flex-col items-center gap-1 py-2 transition-colors',
    isActive ? 'text-text-hi' : 'text-text-lo',
  ].join(' ')

function WatchdogMark() {
  return (
    <span
      aria-hidden="true"
      className="flex h-7 w-7 items-center justify-center rounded-control bg-panel text-mint"
    >
      <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
        <circle cx="10" cy="10" r="6.4" />
        <circle cx="10" cy="10" r="1.8" fill="currentColor" stroke="none" />
      </svg>
    </span>
  )
}

const TABS = [
  { to: '/', label: 'Feed', end: true },
  { to: '/started', label: 'Started', end: false },
  { to: '/applied', label: 'Applied', end: false },
]

function TopBar() {
  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-6 border-b border-line bg-surface px-4 md:px-6">
      <h1 className="shrink-0 text-[20px] font-bold tracking-tight">JOBS</h1>
      <span aria-hidden="true" className="text-text-faint">
        ›
      </span>

      <nav aria-label="Views" className="flex min-w-0 items-center gap-5 overflow-x-auto">
        {TABS.map(({ to, label, end }) => (
          <NavLink key={to} to={to} end={end} className={tabItem}>
            {label}
          </NavLink>
        ))}
      </nav>
    </header>
  )
}

/** The underline is 2px and sits on the header's own bottom border. */
const tabItem = ({ isActive }: { isActive: boolean }) =>
  [
    'shrink-0 border-b-2 pb-[18px] pt-[20px] text-[15px] transition-colors',
    isActive
      ? 'border-text-hi font-semibold text-text-hi'
      : 'border-transparent text-text-mid hover:text-text-hi',
  ].join(' ')
