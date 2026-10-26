import { api } from '../../api';
import { toIsoDate, useAsync, type Schemas } from '../../pipeline/shared';

export type PeopleFilters = {
  q: string;
  types: Schemas['PersonType'][];
  ownerId?: string;
  consent?: Schemas['ConsentStatus'];
  /** Not contacted for at least this many days (0 = off). */
  staleDays: number;
  /** `field,dir`; empty = default order. */
  sort: string;
  page: number;
  tags?: string[];
  channels?: Schemas["SourceChannel"][];
  cv?: string;
  shareable?: boolean;
};

const query = (f: PeopleFilters, page: number, size: number) => {
  const before = f.staleDays ? new Date(Date.now() - f.staleDays * 86_400_000) : undefined;
  return {
    q: f.q || undefined,
    type: f.types.length ? f.types : undefined,
    ownerId: f.ownerId,
    consentStatus: f.consent ? [f.consent] : undefined,
    lastContactBefore: before ? toIsoDate(before) : undefined,
    tag: f.tags?.length ? f.tags : undefined,
    sourceChannel: f.channels?.length ? f.channels : undefined,
    cv: f.cv || undefined,
    shareable: f.shareable ? true : undefined,
    page,
    size,
    // With a cv search the server default (relevance) applies unless a sort is chosen.
    sort: f.sort ? [f.sort] : f.cv ? undefined : [before ? 'lastContactAt,asc' : 'name,asc'],
  };
};

/** Network list query (A-12). With the stale filter on, the oldest contact comes first unless a sort is chosen. */
export function usePeople(f: PeopleFilters) {
  return useAsync(() => api.GET('/people', { params: { query: query(f, f.page, 25) } }), [f.q, f.types.join(','), f.ownerId, f.consent, f.staleDays, f.sort, f.page, f.tags?.join(','), f.channels?.join(','), f.cv, f.shareable]);
}

export const EXPORT_LIMIT = 1000;

/** Every person matching the filters (for CSV export), capped at EXPORT_LIMIT. */
export async function fetchAllPeople(f: PeopleFilters): Promise<{ people: Schemas['PersonSummary'][]; total: number }> {
  const people: Schemas['PersonSummary'][] = [];
  let total = 0;
  for (let page = 0; people.length < EXPORT_LIMIT; page++) {
    const r = await api.GET('/people', { params: { query: query(f, page, 100) } });
    if (!r.data) throw new Error('export failed');
    total = r.data.page.totalElements;
    people.push(...r.data.content);
    if (page + 1 >= r.data.page.totalPages) break;
  }
  return { people: people.slice(0, EXPORT_LIMIT), total };
}
