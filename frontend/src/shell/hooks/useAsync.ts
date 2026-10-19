import { useCallback, useEffect, useRef, useState } from 'react';

type Result<T> = { data?: T; error?: unknown; response?: { status: number } };

/** Runs an openapi-fetch call and tracks loading / error; `reload()` repeats it. */
export function useAsync<T>(fn: () => Promise<Result<T>>, deps: unknown[]) {
  const [state, setState] = useState<{ data?: T; error?: unknown; status?: number; loading: boolean }>({ loading: true });
  const seq = useRef(0);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps);
  const load = useCallback(() => {
    const my = ++seq.current;
    // keep previous data while refining (no blanking)
    setState((s) => ({ data: s.data, loading: true }));
    run().then(
      (r) => {
        if (my !== seq.current) return;
        if (r.error !== undefined || r.data === undefined) setState({ error: r.error ?? new Error('empty'), status: r.response?.status, loading: false });
        else setState({ data: r.data, loading: false });
      },
      (e: unknown) => {
        if (my === seq.current) setState({ error: e, loading: false });
      },
    );
  }, [run]);
  useEffect(() => {
    load();
    return () => {
      seq.current++;
    };
  }, [load]);
  return { ...state, reload: load };
}
