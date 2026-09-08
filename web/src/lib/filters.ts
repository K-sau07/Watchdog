/**
 * Pure filter-state → query-string logic (mirrors the backend PostingQueryMapper params,
 * spec §5/§7). Framework-free and unit-tested (constitution §7): the FilterRail holds this
 * state, this module serializes it for GET /api/postings.
 */

/** UI filter state — every field optional; empty means "no constraint". */
export interface FilterState {
  roles: string[];
  keywords: string[];
  exclude: string[];
  location: string[];
  seniority: string[];
  remote: string[];
  employmentType: string[];
  source: string[];
  state: string[];
  salaryMin: number | null;
  includeUnknownSalary: boolean | null;
  sponsorship: string | null;
  postedWithin: string | null; // token: 10m|30m|1h|today|week
  dateFrom: string | null; // ISO instant
  dateTo: string | null;
  page: number;
  size: number | null;
}

/** The empty filter — matches everything (backend FilterCriteria.all()). */
export const EMPTY_FILTER: FilterState = {
  roles: [],
  keywords: [],
  exclude: [],
  location: [],
  seniority: [],
  remote: [],
  employmentType: [],
  source: [],
  state: [],
  salaryMin: null,
  includeUnknownSalary: null,
  sponsorship: null,
  postedWithin: null,
  dateFrom: null,
  dateTo: null,
  page: 0,
  size: null,
};

/**
 * Serialize filter state to a URLSearchParams query string. Omits empty dimensions so the
 * URL stays clean and the backend reads them as "no constraint". List fields are
 * comma-joined (what PostingQueryMapper.splitList expects).
 */
export function toQueryString(f: FilterState): string {
  const p = new URLSearchParams();
  const list = (key: string, values: string[]) => {
    if (values.length > 0) p.set(key, values.join(','));
  };
  list('roles', f.roles);
  list('keywords', f.keywords);
  list('exclude', f.exclude);
  list('location', f.location);
  list('seniority', f.seniority);
  list('remote', f.remote);
  list('employmentType', f.employmentType);
  list('source', f.source);
  list('state', f.state);
  if (f.salaryMin !== null) p.set('salaryMin', String(f.salaryMin));
  if (f.includeUnknownSalary !== null) p.set('includeUnknownSalary', String(f.includeUnknownSalary));
  if (f.sponsorship) p.set('sponsorship', f.sponsorship);
  if (f.postedWithin) p.set('postedWithin', f.postedWithin);
  if (f.dateFrom) p.set('dateFrom', f.dateFrom);
  if (f.dateTo) p.set('dateTo', f.dateTo);
  if (f.page > 0) p.set('page', String(f.page));
  if (f.size !== null) p.set('size', String(f.size));
  return p.toString();
}

/** Count of active (constraining) dimensions — drives the "N filters on" hint + reset. */
export function activeCount(f: FilterState): number {
  let n = 0;
  n += f.roles.length ? 1 : 0;
  n += f.keywords.length ? 1 : 0;
  n += f.exclude.length ? 1 : 0;
  n += f.location.length ? 1 : 0;
  n += f.seniority.length ? 1 : 0;
  n += f.remote.length ? 1 : 0;
  n += f.employmentType.length ? 1 : 0;
  n += f.source.length ? 1 : 0;
  n += f.state.length ? 1 : 0;
  n += f.salaryMin !== null ? 1 : 0;
  n += f.sponsorship ? 1 : 0;
  n += f.postedWithin ? 1 : 0;
  n += f.dateFrom || f.dateTo ? 1 : 0;
  return n;
}
