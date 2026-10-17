/**
 * Inline 14px line icons for the card's fact grid and actions.
 *
 * Hand-rolled rather than pulled from a library: the set is six glyphs, and a icon
 * package would cost more bytes than the whole feed view. All are decorative — every
 * fact they sit beside carries its own <dt> label, and every icon button an aria-label.
 */

const base = {
  width: 14,
  height: 14,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
}

export function PinIcon() {
  return (
    <svg {...base}>
      <path d="M13 6.8c0 3.4-5 8-5 8s-5-4.6-5-8a5 5 0 1 1 10 0Z" />
      <circle cx="8" cy="6.6" r="1.8" />
    </svg>
  )
}

export function TypeIcon() {
  return (
    <svg {...base}>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 4.8V8l2.1 1.5" />
    </svg>
  )
}

export function MoneyIcon() {
  return (
    <svg {...base}>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 4.4v7.2M9.9 6.2a2 2 0 0 0-1.9-1c-1 0-1.8.5-1.8 1.4 0 2 3.8 1.1 3.8 3.1 0 .9-.8 1.5-1.9 1.5a2.1 2.1 0 0 1-2-1.1" />
    </svg>
  )
}

export function RemoteIcon() {
  return (
    <svg {...base}>
      <path d="M2.6 7.4 8 3l5.4 4.4" />
      <path d="M4.2 8.6V13h7.6V8.6" />
    </svg>
  )
}

export function LevelIcon() {
  return (
    <svg {...base}>
      <path d="M2.4 5.2 5 7.6 8 3.4l3 4.2 2.6-2.4-1 6.4H3.4l-1-6.4Z" />
    </svg>
  )
}

export function BanIcon() {
  return (
    <svg {...base} width={15} height={15}>
      <circle cx="8" cy="8" r="6" />
      <path d="M3.8 3.8l8.4 8.4" />
    </svg>
  )
}

export function HeartIcon({ filled = false }: { filled?: boolean }) {
  return (
    <svg {...base} width={15} height={15} fill={filled ? 'currentColor' : 'none'}>
      <path d="M8 13.2S2.6 10 2.6 6.4a2.9 2.9 0 0 1 5.4-1.5 2.9 2.9 0 0 1 5.4 1.5c0 3.6-5.4 6.8-5.4 6.8Z" />
    </svg>
  )
}
