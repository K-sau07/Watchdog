/**
 * Typed API client for the Watchdog backend (spec §7). Thin fetch wrapper; TanStack
 * Query owns caching/refetch (constitution §7). Types mirror the backend response DTOs
 * exactly — keep them in sync with the Java records in infrastructure/web.
 */

// --- response shapes (mirror the backend DTOs) ---

export interface SalaryDto {
  min: number | null;
  max: number | null;
  currency: string | null;
}

/** Mirrors PostingDtos.PostingSummary. */
export interface PostingSummary {
  id: string;
  title: string;
  companyName: string | null;
  location: string | null;
  remoteType: string;
  employmentType: string;
  seniority: string;
  sponsorshipSignal: string;
  salary: SalaryDto | null;
  url: string | null;
  source: string | null;
  fundingStage: string | null; // company's funding stage; null if the company is unknown
  postedAt: string | null; // ISO instant
  firstSeenAt: string; // ISO instant
  caughtMinutes: number | null; // the signature stat; null when postedAt unknown
}

/** Mirrors PostingDtos.PostingDetail. */
export interface PostingDetail {
  summary: PostingSummary;
  description: string;
}

/** Mirrors PostingDtos.PageResponse<T>. */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalMatched: number;
  totalPages: number;
}

/** Mirrors AgentStatusController.StatusResponse. */
export interface AgentStatus {
  boardsWatched: number;
  greenhouse: number;
  lever: number;
  ashby: number;
  lastPoll: string | null;
  nextPoll: string | null;
  newToday: number;
  medianCatchMinutesToday: number | null;
  /**
   * Postings written today that were posted *after* we started watching their board —
   * the population the median describes. Smaller than newToday whenever a backfill
   * landed, and zero on a day that was all backfill.
   */
  caughtToday: number;
}

/** Mirrors FilterProfileDtos.ProfileResponse. */
export interface FilterProfile {
  id: string;
  name: string;
  roleKeywords: string[];
  includeKeywords: string[];
  excludeKeywords: string[];
  locations: string[];
  seniorities: string[];
  remoteTypes: string[];
  employmentTypes: string[];
  salaryMin: number | null;
  includeUnknownSalary: boolean | null;
  sponsorship: string | null;
  postedWithinSeconds: number | null;
  seenFrom: string | null;
  seenTo: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Job-state write, mirrors JobStateController.StateResponse. */
export type JobState = 'NEW' | 'SAVED' | 'APPLIED' | 'HIDDEN';
export interface JobStateResponse {
  postingId: string;
  state: JobState;
  appliedAt: string | null;
  note: string | null;
  updatedAt: string;
}

// --- fetch core ---

/** A failed request; carries the HTTP status so callers can branch (e.g. 404). */
export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/**
 * API origin. Empty in dev so Vite's proxy handles `/api/...`; in production the
 * dashboard is on Vercel and the API on Render, so they are separate origins and
 * VITE_API_BASE supplies the backend URL at build time.
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? '').replace(/\/$/, '');

function url(path: string): string {
  return `${API_BASE}${path}`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url(path), {
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  });
  if (!res.ok) {
    let message = res.statusText;
    try {
      const body = await res.json();
      if (body && typeof body.error === 'string') message = body.error;
    } catch {
      // non-JSON error body; keep statusText
    }
    throw new ApiError(res.status, message);
  }
  // 204/empty bodies: guard before parsing.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

// --- endpoints ---

export function fetchPostings(query: string): Promise<PageResponse<PostingSummary>> {
  const qs = query ? `?${query}` : '';
  return request<PageResponse<PostingSummary>>(`/api/postings${qs}`);
}

export function fetchPosting(id: string): Promise<PostingDetail> {
  return request<PostingDetail>(`/api/postings/${id}`);
}

export function setJobState(
  id: string,
  state: JobState,
  note?: string,
): Promise<JobStateResponse> {
  return request<JobStateResponse>(`/api/postings/${id}/state`, {
    method: 'PUT',
    body: JSON.stringify({ state, note: note ?? null }),
  });
}

export function fetchAgentStatus(): Promise<AgentStatus> {
  return request<AgentStatus>('/api/agent/status');
}

/** On-demand poll result (D-WD19). */
export interface PollResult {
  status: 'polled' | 'cooldown';
  companiesPolled?: number;
  newPostings?: number;
  retryAfterSeconds?: number;
}

/** Trigger an on-demand refresh. Resolves with 'polled' or 'cooldown' (never throws on 429). */
export async function triggerPoll(): Promise<PollResult> {
  const res = await fetch(url('/api/agent/poll'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  // 429 is an expected outcome (cooldown), not an error — parse it normally.
  const text = await res.text();
  return (text ? JSON.parse(text) : { status: 'polled' }) as PollResult;
}

export function fetchDefaultProfile(): Promise<FilterProfile> {
  return request<FilterProfile>('/api/filter-profiles/default');
}

export function updateProfile(
  id: string,
  body: Partial<FilterProfile> & { name: string },
): Promise<FilterProfile> {
  return request<FilterProfile>(`/api/filter-profiles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

// --- outreach ---

/**
 * Where a person stands, in the vocabulary of SaurabhBrain/Pipeline/core/STAGES.md.
 *
 * Derived from the event log rather than stored, except ASKED / WON / DEAD / NURTURE,
 * which a human declares — nothing in a log says "they sounded irritated".
 */
export type OutreachStage =
  | 'REACHED_OUT'
  | 'LIVE'
  | 'EXCHANGE'
  | 'ASKED'
  | 'WON'
  | 'NURTURE'
  | 'DEAD';

/** What this person can give. Decides the shape of the stage-5 ask (ROUTER.md). */
export type CanGive = 'REFER' | 'INTERVIEW' | 'ROUTE';

/** Who they are. Sets register and research depth, not the ask (ROUTER.md). */
export type PersonType =
  | 'ENGINEER'
  | 'RECRUITER'
  | 'HIRING_MANAGER'
  | 'ALUM'
  | 'SENIOR_OFF_REQ';

/**
 * LINKEDIN_INVITE is separate from LINKEDIN_DM because an invite is not a message and
 * must not be charged against the unanswered cap.
 */
export type Channel = 'LINKEDIN_INVITE' | 'LINKEDIN_DM' | 'EMAIL';

export interface OutreachPerson {
  id: string;
  companyName: string;
  personName: string;
  personTitle: string | null;
  linkedinUrl: string | null;
  email: string | null;
  personType: PersonType | null;
  canGive: CanGive | null;
  stage: OutreachStage;
  /** True when a human stated the stage rather than it being derived. */
  stageDeclared: boolean;
  turns: number;
  /** The worst channel's unanswered count — the cap is per channel. */
  unanswered: number;
  channelsTouched: Channel[];
  unansweredPerChannel: Partial<Record<Channel, number>>;
  reachedOutAt: string;
  lastOutboundAt: string | null;
  lastInboundAt: string | null;
  /** Null unless a chase is owed; otherwise the channel to send it on. */
  followUpDueOn: Channel | null;
  postingId: string | null;
}

/** One company tag, its roster, and the two groups the board splits into. */
export interface OutreachCompany {
  companyName: string;
  fundingStage: string | null;
  /** People we aim to reach at each company. */
  target: number;
  invited: number;
  emailed: number;
  awaitingReply: OutreachPerson[];
  inConversation: OutreachPerson[];
}

/** This week's spend against the invite allowance. */
export interface OutreachPace {
  invitesSent: number;
  inviteCap: number;
  companiesTouched: number;
  companiesTarget: number;
  invitesBurned: boolean;
}

export function fetchOutreach(): Promise<OutreachCompany[]> {
  return request<OutreachCompany[]>('/api/outreach');
}

export function fetchFollowUps(): Promise<OutreachPerson[]> {
  return request<OutreachPerson[]>('/api/outreach/follow-ups');
}

export function fetchPace(): Promise<OutreachPace> {
  return request<OutreachPace>('/api/outreach/pace');
}

/** Put someone on a roster and record the first touch in one call. */
export function targetPerson(input: {
  companyName: string;
  personName: string;
  personTitle?: string | null;
  linkedinUrl?: string | null;
  email?: string | null;
  personType?: PersonType | null;
  postingId?: string | null;
  channel?: Channel;
  kind?: string;
}): Promise<OutreachPerson> {
  return request<OutreachPerson>('/api/outreach', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

/** Record a touch or a reply against someone already on a roster. */
export function recordEvent(
  id: string,
  input: {
    direction?: 'OUTBOUND' | 'INBOUND';
    channel?: Channel;
    kind?: string;
    status?: 'DRAFTED' | 'SENT';
    body?: string | null;
  },
): Promise<OutreachPerson> {
  return request<OutreachPerson>(`/api/outreach/${encodeURIComponent(id)}/events`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function patchContact(
  id: string,
  patch: {
    stageOverride?: OutreachStage | null;
    canGive?: CanGive | null;
    email?: string | null;
    personType?: PersonType | null;
  },
): Promise<OutreachPerson> {
  return request<OutreachPerson>(`/api/outreach/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}
