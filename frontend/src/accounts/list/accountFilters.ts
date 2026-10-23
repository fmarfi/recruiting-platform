import { formatSort, type SortState } from '../../shell/grid';
import type { AccountFilters } from './useAccounts';

const STATUSES = ['LEAD', 'PROPOSAL_SENT', 'ACTIVE', 'ON_HOLD', 'CLOSED'];
export const DEFAULT_SORT = 'lastActivityAt,desc';

/**
 * Filters carried in the URL, e.g. /accounts?owner=me&status=ACTIVE&q=holding (written by the command bar).
 * owner: "me" or absent = my accounts (the default), "all" = everybody. status: one account status constant.
 */
export function filtersFromParams(params: URLSearchParams, meId: string): AccountFilters {
  const status = (params.get('status') ?? '').toUpperCase();
  return {
    q: params.get('q') ?? '',
    status: STATUSES.includes(status) ? status : '',
    ownerId: params.get('owner') === 'all' ? '' : meId,
    page: 0,
    sort: DEFAULT_SORT,
  };
}

/** What a saved view stores for the accounts list. The owner is kept as a scope so a view works for any user. */
export interface AccountView {
  q: string;
  status: string;
  owner: 'me' | 'all';
}

export const toView = (f: AccountFilters): AccountView => ({ q: f.q, status: f.status, owner: f.ownerId ? 'me' : 'all' });

export function applyView(f: AccountFilters, v: { filters: AccountView; sort?: SortState | null }, meId: string): AccountFilters {
  return { ...f, q: v.filters.q ?? '', status: v.filters.status ?? '', ownerId: v.filters.owner === 'all' ? '' : meId, page: 0, sort: v.sort ? formatSort(v.sort) : f.sort };
}

