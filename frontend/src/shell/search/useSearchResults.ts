import { api } from '../../api';
import { useAsync } from '../hooks/useAsync';

export type SearchType = 'all' | 'ACCOUNT' | 'PERSON' | 'JOB_ORDER';

export interface SearchFilters {
  q: string;
  type: SearchType;
  sector: string;
  accountStatus: string;
}

/** Full results for /search (up to 50 per group); the page shows 5 per group and expands in place. */
export function useSearchResults(f: SearchFilters) {
  return useAsync(
    () =>
      api.GET('/search', {
        params: {
          query: {
            q: f.q,
            limit: 50,
            sector: f.sector || undefined,
            accountStatus: (f.accountStatus || undefined) as 'LEAD' | undefined,
            types: f.type === 'all' ? undefined : ([f.type] as ['ACCOUNT']),
          },
        },
      }),
    [f.q, f.type, f.sector, f.accountStatus],
  );
}
