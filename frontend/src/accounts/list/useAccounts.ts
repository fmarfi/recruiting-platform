import { api } from '../../api';
import { useAsync } from '../../shell/hooks/useAsync';

export interface AccountFilters {
  q: string;
  status: string; // '' = all
  ownerId: string; // '' = everybody
  page: number;
  sort: string; // e.g. lastActivityAt,desc
}

type Status = 'LEAD' | 'PROPOSAL_SENT' | 'ACTIVE' | 'ON_HOLD' | 'CLOSED';

/** GET /accounts for the list screen (A-01). */
export function useAccounts(f: AccountFilters) {
  return useAsync(
    () =>
      api.GET('/accounts', {
        params: {
          query: {
            q: f.q || undefined,
            status: f.status ? [f.status as Status] : undefined,
            ownerId: f.ownerId || undefined,
            page: f.page,
            size: 20,
            sort: [f.sort],
          },
        },
      }),
    [f.q, f.status, f.ownerId, f.page, f.sort],
  );
}
