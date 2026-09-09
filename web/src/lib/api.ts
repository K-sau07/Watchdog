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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
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
  const res = await fetch('/api/agent/poll', {
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
