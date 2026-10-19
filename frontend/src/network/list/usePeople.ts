import { api } from '../../api';
import { toIsoDate, useAsync, type Schemas } from '../../pipeline/shared';

export type PeopleFilters = {
  q: string;
  types: Schemas['PersonType'][];
  ownerId?: string;
  consent?: Schemas['ConsentStatus'];
  /** Not contacted for at least this many days (0 = off). */
  staleDays: number;
  page: number;
};

/** Network list query (A-12). With the stale filter on, the oldest contact comes first. */
export function usePeople(f: PeopleFilters) {
  return useAsync(() => {
    const before = f.staleDays ? new Date(Date.now() - f.staleDays * 86_400_000) : undefined;
    return api.GET('/people', {
      params: {
        query: {
          q: f.q || undefined,
          type: f.types.length ? f.types : undefined,
          ownerId: f.ownerId,
          consentStatus: f.consent ? [f.consent] : undefined,
          lastContactBefore: before ? toIsoDate(before) : undefined,
          page: f.page,
          size: 25,
          sort: [before ? 'lastContactAt,asc' : 'name,asc'],
        },
      },
    });
  }, [f.q, f.types.join(','), f.ownerId, f.consent, f.staleDays, f.page]);
}
