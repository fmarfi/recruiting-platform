import { api } from '../../../api';
import { useAsync, type Schemas } from '../../shared';

export type JobOrderFilters = {
  q: string;
  status: Schemas['JobOrderStatus'][];
  openOnly: boolean;
  ownerId?: string;
  page: number;
};

/** Job orders for the list screen; defaults are open jobs owned by me (set by the screen). */
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
            sort: ['openedOn,desc'],
          },
        },
      }),
    [f.q, f.status.join(','), f.openOnly, f.ownerId, f.page],
  );
}
