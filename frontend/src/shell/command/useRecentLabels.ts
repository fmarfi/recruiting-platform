import { useEffect, useState } from 'react';
import { api } from '../../api';
import { loadRecentRefs, recentPath, type RecentRef } from './recentItems';

export interface RecentLabel {
  ref: RecentRef;
  to: string;
  title: string;
  sub?: string;
}

async function label(r: RecentRef): Promise<RecentLabel | null> {
  const to = recentPath(r);
  const params = { params: { path: { id: r.id } } };
  try {
    if (r.kind === 'account') {
      const { data } = await api.GET('/accounts/{id}', params);
      return data ? { ref: r, to, title: data.name, sub: [data.sector, data.city].filter(Boolean).join(' - ') } : null;
    }
    if (r.kind === 'person') {
      const { data } = await api.GET('/people/{id}', params);
      return data ? { ref: r, to, title: data.name, sub: [data.title, data.company].filter(Boolean).join(', ') } : null;
    }
    const { data } = await api.GET('/job-orders/{id}', params);
    return data ? { ref: r, to, title: data.title, sub: data.account.name } : null;
  } catch {
    return null;
  }
}

/** Recent records for the empty command bar, with names fetched fresh (so permission changes and deletions apply). */
export function useRecentLabels(userId: string, active: boolean): RecentLabel[] {
  const [items, setItems] = useState<RecentLabel[]>([]);
  useEffect(() => {
    if (!active) return;
    let alive = true;
    void Promise.all(loadRecentRefs(userId).map(label)).then((r) => alive && setItems(r.filter((x): x is RecentLabel => !!x)));
    return () => {
      alive = false;
    };
  }, [userId, active]);
  return items;
}
