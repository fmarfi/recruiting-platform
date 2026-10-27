import { api } from '../../../api';
import { useAsync, type Schemas } from '../../shared';

export type JobOrderFilters = {
  q: string;
  status: Schemas['JobOrderStatus'][];
  openOnly: boolean;
  ownerId?: string;
  page: number;
  /** ield,dir; empty = openedOn,desc. */
  sort?: string;
};

/** Job orders for the list screen; defaults are open jobs owned by me (set by the screen). */
/** The backend rejects `daysOpen` as a sort key (change request in the hand-off); days open is the opposite order of the opened date. */
const backendSort = (s: string) => (s.startsWith('daysOpen') ? (s.endsWith('desc') ? 'openedOn,asc' : 'openedOn,desc') : s);

export function useJobOrders(f: JobOrderFilters) {
  return useAsync(
    () =>
      api.GET('/job-orders', {
        params: {
          query: {
            q: f.q || undefined,
            status: f.status.length ? f.status : undefined,
            open: f.status.length ? undefined : f.openOnly ? true : undefined,
            ownerId: f.ownerId,
            page: f.page,
            size: 20,
            sort: [backendSort(f.sort || 'openedOn,desc')],
          },
        },
      }),
    [f.q, f.status.join(','), f.openOnly, f.ownerId, f.page, f.sort],
  );
}

