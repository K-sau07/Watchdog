/**
 * 18px line glyphs for the rail and the shell chrome.
 *
 * Same reasoning as the card's icon set in features/feed/icons.tsx — five glyphs don't
 * justify a dependency. All decorative: every one sits beside its own text label.
 */

const base = {
  width: 18,
  height: 18,
  viewBox: '0 0 20 20',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
}

/** The feed — a radar sweep, which is what the agent actually does. */
export function RadarIcon() {
  return (
    <svg {...base}>
      <circle cx="10" cy="10" r="7.2" />
      <circle cx="10" cy="10" r="3.2" />
      <path d="M10 10 14.8 5.6" />
    </svg>
  )
}

/** Started conversations — two speech turns, not one. */
export function ConversationIcon() {
  return (
    <svg {...base}>
      <path d="M3 6.4a1.8 1.8 0 0 1 1.8-1.8h6.1a1.8 1.8 0 0 1 1.8 1.8v3a1.8 1.8 0 0 1-1.8 1.8H7.2L4.3 13.3v-2.1H4.8A1.8 1.8 0 0 1 3 9.4Z" />
      <path d="M15.9 8.2h.3A1.8 1.8 0 0 1 18 10v3a1.8 1.8 0 0 1-1.8 1.8h-.5L12.8 17v-2.2" />
    </svg>
  )
}

/** Applied — a page with a check on it. */
export function AppliedIcon() {
  return (
    <svg {...base}>
      <path d="M11 2.6H5.8A1.8 1.8 0 0 0 4 4.4v11.2a1.8 1.8 0 0 0 1.8 1.8h8.4a1.8 1.8 0 0 0 1.8-1.8V7.4Z" />
      <path d="M11 2.6v3.6a1.2 1.2 0 0 0 1.2 1.2H16" />
      <path d="m7.4 12.4 1.8 1.8 3.4-3.6" />
    </svg>
  )
}

/** The agent itself — a watching eye. */
export function AgentIcon() {
  return (
    <svg {...base}>
      <path d="M1.8 10S5 4.4 10 4.4 18.2 10 18.2 10 15 15.6 10 15.6 1.8 10 1.8 10Z" />
      <circle cx="10" cy="10" r="2.4" />
    </svg>
  )
}

export function SettingsIcon() {
  return (
    <svg {...base}>
      <circle cx="10" cy="10" r="2.6" />
      <path d="M16.1 12.4a1.3 1.3 0 0 0 .26 1.44l.05.05a1.6 1.6 0 1 1-2.26 2.26l-.05-.05a1.3 1.3 0 0 0-1.44-.26 1.3 1.3 0 0 0-.79 1.19v.13a1.6 1.6 0 1 1-3.2 0v-.07a1.3 1.3 0 0 0-.85-1.19 1.3 1.3 0 0 0-1.44.26l-.05.05a1.6 1.6 0 1 1-2.26-2.26l.05-.05a1.3 1.3 0 0 0 .26-1.44 1.3 1.3 0 0 0-1.19-.79h-.13a1.6 1.6 0 1 1 0-3.2h.07a1.3 1.3 0 0 0 1.19-.85 1.3 1.3 0 0 0-.26-1.44l-.05-.05a1.6 1.6 0 1 1 2.26-2.26l.05.05a1.3 1.3 0 0 0 1.44.26h.06a1.3 1.3 0 0 0 .79-1.19v-.13a1.6 1.6 0 1 1 3.2 0v.07a1.3 1.3 0 0 0 .79 1.19 1.3 1.3 0 0 0 1.44-.26l.05-.05a1.6 1.6 0 1 1 2.26 2.26l-.05.05a1.3 1.3 0 0 0-.26 1.44v.06a1.3 1.3 0 0 0 1.19.79h.13a1.6 1.6 0 1 1 0 3.2h-.07a1.3 1.3 0 0 0-1.19.79Z" />
    </svg>
  )
}

/** Caret for the filter pills. */
export function CaretIcon() {
  return (
    <svg width="10" height="10" viewBox="0 0 12 12" fill="none" aria-hidden="true">
      <path d="m3 4.6 3 3 3-3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
