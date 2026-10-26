import { useMemo, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { createViewStore, type ViewNotice, type ViewStoreOptions } from './viewStoreFactory';
import type { ViewStore } from './types';

/**
 * React wrapper for the server-backed ViewStore: knows the user (own views, SHARED only for MANAGER/ADMIN)
 * and collects the notices the screen must show (pass `notice` to ViewsMenu).
 * `options` must be stable (module constant or memoised), it is read once per user.
 */
export function useViewStore<F>(options: Omit<ViewStoreOptions<F>, 'userId' | 'canShare' | 'onNotice'>): { store: ViewStore<F>; notice: ViewNotice | null } {
  const { user, isManager } = useAuth();
  const [notice, setNotice] = useState<ViewNotice | null>(null);
  const userId = user?.id;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const store = useMemo(() => createViewStore<F>({ ...options, userId, canShare: isManager, onNotice: setNotice }), [userId, isManager, options.gridId, options.entity]);
  return { store, notice };
}
