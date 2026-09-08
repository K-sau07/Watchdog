/**
 * The signature stat's brain (bible §2 freshness ramp, spec §6 "caught N min after
 * posting"). Pure and framework-free so it's unit-tested without React (constitution §7).
 *
 * A posting's freshness is derived from its catch-time in whole minutes — how long after
 * it posted Watchdog caught it — NOT from wall-clock age. Newer catches burn hotter; a
 * catch with no known postedAt is honestly "unknown" and never faked a temperature.
 */

/** Freshness temperature buckets → drive card glow + catch-stamp color. */
export type Temperature = 'hot' | 'steady' | 'dim' | 'cooled' | 'unknown';

/** Boundaries in minutes (bible §2). */
export const FRESH_HOT_MAX = 2; // < 2 min
export const FRESH_STEADY_MAX = 15; // 2–15 min
export const FRESH_DIM_MAX = 60; // 15–60 min; > 60 → cooled

/**
 * Bucket a catch-time (whole minutes) into a temperature.
 * @param catchMinutes minutes after posting that we caught it; null = unknown postedAt.
 */
export function temperatureOf(catchMinutes: number | null): Temperature {
  if (catchMinutes === null) return 'unknown';
  if (catchMinutes < FRESH_HOT_MAX) return 'hot';
  if (catchMinutes < FRESH_STEADY_MAX) return 'steady';
  if (catchMinutes < FRESH_DIM_MAX) return 'dim';
  return 'cooled';
}

/**
 * Human label for the catch stamp: "caught 2 min", "caught 3 hr", "caught 1 day",
 * or "caught —" when unknown. Honest per spec §8.4 — never invents a duration.
 */
export function catchLabel(catchMinutes: number | null): string {
  if (catchMinutes === null) return 'caught —';
  if (catchMinutes < 1) return 'caught <1 min';
  if (catchMinutes < 60) return `caught ${catchMinutes} min`;
  if (catchMinutes < 60 * 24) {
    const hr = Math.floor(catchMinutes / 60);
    return `caught ${hr} hr`;
  }
  const days = Math.floor(catchMinutes / (60 * 24));
  return `caught ${days} day${days === 1 ? '' : 's'}`;
}

/** True only for the hottest bucket — the one that gets the entry pulse animation. */
export function isFresh(catchMinutes: number | null): boolean {
  return temperatureOf(catchMinutes) === 'hot';
}
