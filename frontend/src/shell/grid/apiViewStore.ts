import { api as defaultApi, type components } from '../../api';
import type { ColumnState, NewView, SavedView, SortState, ViewStore } from './types';
import { mapViewError, ViewStoreError } from './viewErrors';

type ApiClient = typeof defaultApi;
type ApiView = components['schemas']['SavedView'];
type Definition = components['schemas']['ViewDefinition'];
type Entity = components['schemas']['ListEntity'];
export type FilterRecord = Definition['filters'];

export interface ApiViewStoreOptions<F> {
  entity: Entity;
  client?: ApiClient;
  /** Current user id, to mark own views. */
  userId?: string;
  /** MANAGER/ADMIN: SHARED views are offered (the server enforces it too). */
  canShare?: boolean;
  /** Screen filters to /views filter keys (default: the object as is, empty values left out). */
  toFilters?: (f: F) => FilterRecord;
  fromFilters?: (r: FilterRecord) => F;
  /** True when this filter set holds free text, which SHARED views do not keep. */
  hasFreeText?: (f: F) => boolean;
}

const empty = (v: unknown) => v === '' || v === null || v === undefined || (Array.isArray(v) && v.length === 0);

export function defaultToFilters<F>(f: F): FilterRecord {
  const out: FilterRecord = {};
  for (const [k, v] of Object.entries((f ?? {}) as Record<string, unknown>)) if (!empty(v)) out[k] = v as FilterRecord[string];
  return out;
}

/** Visible columns in display order, as the /views `columns` list. */
export function columnsToApi(c: ColumnState): string[] {
  const rank = (id: string) => (c.order.includes(id) ? c.order.indexOf(id) : Number.MAX_SAFE_INTEGER);
  return [...c.visible].sort((a, b) => rank(a) - rank(b));
}
export const columnsFromApi = (cols: string[]): ColumnState => ({ order: cols, visible: cols });
export const sortToApi = (s: SortState | null | undefined): string[] => (s ? [`${s.field},${s.dir}`] : []);
export function sortFromApi(list: string[]): SortState | null {
  const [field, dir] = (list[0] ?? '').split(',');
  return field ? { field, dir: dir === 'desc' ? 'desc' : 'asc' } : null;
}

/**
 * ViewStore over GET/POST /views and PATCH/DELETE /views/{id}. Network failures and 5xx raise
 * ViewStoreError('UNAVAILABLE') so a caller can fall back to the local store; other codes are user-facing
 * (DUPLICATE, LIMIT, CONFLICT, FORBIDDEN...). Versions are kept from the last list for optimistic locking.
 */
export function apiViewStore<F = unknown>(opts: ApiViewStoreOptions<F>): ViewStore<F> {
  const client = opts.client ?? defaultApi;
  const toFilters = opts.toFilters ?? defaultToFilters;
  const fromFilters = opts.fromFilters ?? ((r: FilterRecord) => r as unknown as F);
  const versions = new Map<string, number>();

  const fromApi = (v: ApiView): SavedView<F> => {
    versions.set(v.id, v.version);
    return {
      id: v.id,
      name: v.name,
      filters: fromFilters(v.definition.filters),
      columns: v.definition.columns.length ? columnsFromApi(v.definition.columns) : undefined,
      sort: sortFromApi(v.definition.sort),
      visibility: v.visibility,
      editable: v.editable,
      mine: opts.userId ? v.owner.id === opts.userId : undefined,
      ownerName: v.owner.name,
      version: v.version,
    };
  };
  const definition = (v: Pick<NewView<F>, 'filters' | 'columns' | 'sort'>): Definition => ({
    filters: toFilters(v.filters),
    columns: v.columns ? columnsToApi(v.columns) : [],
    sort: sortToApi(v.sort),
  });
  /** Runs a request; thrown fetch errors count as unavailable. */
  const call = async <T>(run: () => Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T | undefined> => {
    let r;
    try {
      r = await run();
    } catch {
      throw new ViewStoreError('UNAVAILABLE');
    }
    if (r.error !== undefined || !r.response.ok) throw mapViewError(r.response.status, r.error);
    return r.data;
  };
  const guardShare = (visibility?: string) => {
    if (visibility === 'SHARED' && !opts.canShare) throw new ViewStoreError('FORBIDDEN', 403);
  };

  return {
    canShare: !!opts.canShare,
    warnsOnShare: (f) => !!opts.hasFreeText?.(f),
    async list() {
      const data = await call(() => client.GET('/views', { params: { query: { entity: opts.entity } } }));
      return (data ?? []).map(fromApi);
    },
    async save(view) {
      guardShare(view.visibility);
      const id = view.id;
      if (id) {
        const data = await call(() =>
          client.PATCH('/views/{id}', {
            params: { path: { id } },
            body: { name: view.name, definition: definition(view), ...(view.visibility ? { visibility: view.visibility } : {}), version: versions.get(id) },
          }),
        );
        return fromApi(data!);
      }
      const data = await call(() =>
        client.POST('/views', { body: { entity: opts.entity, name: view.name, visibility: view.visibility ?? 'PRIVATE', definition: definition(view) } }),
      );
      return fromApi(data!);
    },
    async rename(id, name) {
      const data = await call(() => client.PATCH('/views/{id}', { params: { path: { id } }, body: { name, version: versions.get(id) } }));
      return fromApi(data!);
    },
    async delete(id) {
      await call(() => client.DELETE('/views/{id}', { params: { path: { id } } }));
      versions.delete(id);
    },
  };
}
