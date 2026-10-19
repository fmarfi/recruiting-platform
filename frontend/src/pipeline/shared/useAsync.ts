import { useCallback, useEffect, useState } from 'react';

/** Load once per call of `fn`; `reload()` refetches without clearing the data. */
export function useAsync<T>(fn: () => Promise<{ data?: T; error?: unknown }>, deps: unknown[]) {
  const [state, setState] = useState<{ data?: T; error?: unknown; loading: boolean }>({ loading: true });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let live = true;
    fn()
      .then((r) => live && setState({ data: r.data, error: r.error, loading: false }))
      .catch((e: unknown) => live && setState({ error: e ?? new Error('network'), loading: false }));
    return () => {
      live = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}

