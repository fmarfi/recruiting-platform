// Job orders list state <-> URL: /job-orders?owner=me|all&status=OPEN,OFFER&q=cfo&page=2&sort=title,asc (the command bar writes the same params).
import { formatSort, parseSort } from '../../../shell/grid';
import type { Schemas } from '../../shared';

export type JobListState = { q: string; status: Schemas['JobOrderStatus'][]; mine: boolean; sort: string; page: number };

export const JOB_STATUSES: Schemas['JobOrderStatus'][] = ['OPEN', 'SOURCING', 'SHORTLIST_SENT', 'INTERVIEWING', 'OFFER', 'FILLED', 'CANCELLED'];
export const JOB_SORT_FIELDS = ['title', 'status', 'openedOn', 'daysOpen'];
export const DEFAULT_JOB_SORT = 'openedOn,desc';

/** owner: absent or "me" = my jobs (the default), "all" = everybody. status: comma separated and/or repeated; unknown values are dropped. */
export function jobStateFromParams(params: URLSearchParams, defaultAll = false): JobListState {
  const status = [...new Set(params.getAll('status').join(',').split(',').map((x) => x.trim().toUpperCase()))].filter((x): x is Schemas['JobOrderStatus'] => (JOB_STATUSES as string[]).includes(x));
  const sort = parseSort(params.get('sort'));
  const page = Number(params.get('page'));
  return {
    q: params.get('q') ?? '',
    status,
    mine: (params.getAll('owner').pop()?.toLowerCase() ?? (defaultAll ? 'all' : 'me')) !== 'all',
    sort: sort && JOB_SORT_FIELDS.includes(sort.field) ? formatSort(sort) : DEFAULT_JOB_SORT,
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
  };
}

export function jobParamsFromState(s: JobListState, defaultAll = false): URLSearchParams {
  const p = new URLSearchParams();
  if (defaultAll ? s.mine : !s.mine) p.set('owner', defaultAll ? 'me' : 'all');
  if (s.status.length) p.set('status', s.status.join(','));
  if (s.q) p.set('q', s.q);
  if (s.sort !== DEFAULT_JOB_SORT) p.set('sort', s.sort);
  if (s.page > 0) p.set('page', String(s.page + 1));
  return p;
}
