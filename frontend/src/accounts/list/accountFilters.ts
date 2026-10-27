import { formatSort, parseSort, type SortState } from '../../shell/grid';
import type { AccountFilters } from './useAccounts';

const STATUSES = ['LEAD', 'PROPOSAL_SENT', 'ACTIVE', 'ON_HOLD', 'CLOSED'];
const SORT_FIELDS = ['name', 'status', 'openedOn', 'lastActivityAt', 'openJobCount'];
export const DEFAULT_SORT = 'lastActivityAt,desc';

/**
 * The accounts list state lives in the URL, e.g. /accounts?owner=me&status=ACTIVE&q=holding&page=2&sort=name,asc
 * (the command bar writes the same params). owner: absent or "me" = my accounts (the default), "all" = everybody.
 * status: one account status constant (a repeated or comma separated list keeps the last one, the list takes a single status).
 * page is 1-based; defaults are left out of the URL.
 */
export function filtersFromParams(params: URLSearchParams, meId: string, defaultAll = false): AccountFilters {
  const raw = params.getAll('status').join(',').split(',').pop() ?? '';
  const status = raw.trim().toUpperCase();
  const sort = parseSort(params.get('sort'));
  const page = Number(params.get('page'));
  return {
    q: params.get('q') ?? '',
    status: STATUSES.includes(status) ? status : '',
    ownerId: (params.get('owner') ?? (defaultAll ? 'all' : 'me')) === 'all' ? '' : meId,
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
    sort: sort && SORT_FIELDS.includes(sort.field) ? formatSort(sort) : DEFAULT_SORT,
  };
}

/** `defaultAll`: the default scope is everybody (admins own no records), so only "me" is written. */
export function paramsFromFilters(f: AccountFilters, defaultAll = false): URLSearchParams {
  const p = new URLSearchParams();
  if (defaultAll ? !!f.ownerId : !f.ownerId) p.set('owner', defaultAll ? 'me' : 'all');
  if (f.status) p.set('status', f.status);
  if (f.q) p.set('q', f.q);
  if (f.page > 0) p.set('page', String(f.page + 1));
  if (f.sort !== DEFAULT_SORT) p.set('sort', f.sort);
  return p;
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

/** Server-backed saved views (/views, entity ACCOUNTS): q, status and ownerId ("me") are the allowed filter keys. */
export const VIEW_OPTIONS = {
  gridId: 'accounts',
  entity: 'ACCOUNTS',
  toFilters: (v: AccountView) => ({ ...(v.q ? { q: v.q } : {}), ...(v.status ? { status: v.status } : {}), ...(v.owner === 'all' ? {} : { ownerId: 'me' }) }),
  fromFilters: (r: Record<string, unknown>): AccountView => ({
    q: typeof r.q === 'string' ? r.q : '',
    status: Array.isArray(r.status) ? String(r.status[0] ?? '') : typeof r.status === 'string' ? r.status : '',
    owner: r.ownerId === undefined ? 'all' : 'me',
  }),
  hasFreeText: (v: AccountView) => !!v.q,
} as const;
