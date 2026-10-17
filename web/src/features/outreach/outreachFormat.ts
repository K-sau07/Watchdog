import type { Channel, OutreachPerson, OutreachStage, PersonType } from '../../lib/api';

/**
 * Pure read-outs for the Started board. Framework-free and unit-tested, like
 * freshness.ts and statusFormat.ts before it (constitution §7).
 *
 * The backend owns the rules — what a stage is, whether a chase is owed. This file only
 * turns those answers into words, so there is no second implementation of the cap to
 * disagree with the first.
 */

export const STAGE_LABEL: Record<OutreachStage, string> = {
  REACHED_OUT: 'no reply yet',
  LIVE: 'replied',
  EXCHANGE: 'in conversation',
  ASKED: 'asked',
  WON: 'won',
  NURTURE: 'nurture',
  DEAD: 'dead',
};

/** Short enough to sit in a chip beside a name. */
export const TYPE_LABEL: Record<PersonType, string> = {
  ENGINEER: 'engineer',
  RECRUITER: 'recruiter',
  HIRING_MANAGER: 'hiring manager',
  ALUM: 'alum',
  SENIOR_OFF_REQ: 'senior, off req',
};

export const CHANNEL_LABEL: Record<Channel, string> = {
  LINKEDIN_INVITE: 'in',
  LINKEDIN_DM: 'in',
  EMAIL: 'email',
};

/** Has this person been reached on LinkedIn at all — invite or DM. */
export function touchedLinkedIn(person: OutreachPerson): boolean {
  return person.channelsTouched.some((c) => c !== 'EMAIL');
}

export function touchedEmail(person: OutreachPerson): boolean {
  return person.channelsTouched.includes('EMAIL');
}

/** The chip row beside a name: which doors we have actually knocked on. */
export function channelChips(person: OutreachPerson): string[] {
  const chips: string[] = [];
  if (touchedLinkedIn(person)) chips.push('in');
  if (touchedEmail(person)) chips.push('email');
  return chips;
}

export type Owing = 'chase' | 'you' | 'them' | 'stop' | 'none';

/**
 * What this row is waiting on.
 *
 * A chase and an answer are different obligations and the board must not blur them: one
 * is "they went quiet, go again", the other is "they wrote, write back". Only the second
 * has a person on the other end expecting something.
 */
export function owes(person: OutreachPerson): Owing {
  if (person.stage === 'DEAD' || person.stage === 'WON') return 'none';
  if (person.stage === 'NURTURE') return 'stop';
  if (person.followUpDueOn !== null) return 'chase';
  // They spoke last — anything inbound after our last outbound means the ball is ours.
  if (person.lastInboundAt !== null) {
    if (person.lastOutboundAt === null) return 'you';
    return Date.parse(person.lastInboundAt) > Date.parse(person.lastOutboundAt) ? 'you' : 'them';
  }
  return 'them';
}

export function owingLabel(owing: Owing, person?: OutreachPerson): string | null {
  switch (owing) {
    case 'chase':
      return person?.followUpDueOn === 'EMAIL' ? 'follow up by email' : 'follow up today';
    case 'you':
      return 'your turn';
    case 'them':
      return 'waiting on them';
    case 'stop':
      return 'every channel spent — comments only';
    case 'none':
      return null;
  }
}

/** "3 days ago" / "today", against a caller-supplied now so tests stay deterministic. */
export function sinceLabel(iso: string | null, nowMs: number): string {
  if (!iso) return '—';
  const days = Math.floor((nowMs - Date.parse(iso)) / 86_400_000);
  if (days <= 0) return 'today';
  if (days === 1) return 'yesterday';
  return `${days}d ago`;
}

/** The company header line: how much of the target this company covers. */
export function coverageLabel(company: {
  target: number;
  invited: number;
  emailed: number;
  awaitingReply: unknown[];
  inConversation: unknown[];
}): string {
  const roster = company.awaitingReply.length + company.inConversation.length;
  return `${roster} of ${company.target} targeted · ${company.invited} on linkedin · ${company.emailed} emailed`;
}

/** Pace strip: "160 / 200 invites · 18 of 20 companies". */
export function paceLabel(pace: {
  invitesSent: number;
  inviteCap: number;
  companiesTouched: number;
  companiesTarget: number;
}): string {
  return `${pace.invitesSent} / ${pace.inviteCap} invites · ${pace.companiesTouched} of ${pace.companiesTarget} companies`;
}
